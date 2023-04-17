package zioworld

import zio.*
import cats.data.Reader

object ReaderMonadExample extends ZIOAppDefault:

  def run =
    val dbClient = new DbClient(ConnectionPool.create())
    val customerRepo = new CustomerRepository(dbClient)
    val logger = new Logger()
    val emailClient = new EmailClient(logger)
    val advertisementService = new AdvertisementService()
    val customerService = new CustomerService(customerRepo, logger)

    CashierApi
      .processLoyaltyCard("12345")
      .run(CashierApi(customerService, advertisementService, emailClient))

  case class CashierApi(
      customerService: CustomerService,
      advertisementService: AdvertisementService,
      emailClient: EmailClient
  )

  object CashierApi:
    def processLoyaltyCard(cardId: String) = Reader { (casierApi: CashierApi) =>
      for {
        customer <- casierApi.customerService
          .findCustomerByCardId(cardId)
          .someOrFail(CustomerNotFound(cardId))
        coupons <- casierApi.advertisementService.findCouponsFor(customer.id)
        _ <- casierApi.emailClient.sendCoupons(customer.email, coupons)
      } yield ()
    }

  class DbClient(connectoonPool: ConnectionPool)

  class CustomerRepository(db: DbClient):
    def findById(id: String): ZIO[Any, Nothing, Option[Customer]] =
      ZIO.some(Customer(id, "first_name", "name@email"))

  class CustomerService(customerRepo: CustomerRepository, logger: Logger):
    def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]] =
      customerRepo.findById(id)

  class Logger():
    def log(s: String): Task[Unit] = ZIO.log(s)

  class EmailClient(logger: Logger):
    def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] =
      logger.log(
        s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
      )

  class AdvertisementService():
    def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
      ZIO.succeed(
        Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
      )

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  object ConnectionPool:
    def create(): ConnectionPool = new ConnectionPool()

  class ConnectionPool():
    def close = ???
  
  case class CustomerNotFound(id: String) extends Throwable
