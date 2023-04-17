package zioworld

import zio.*
import com.softwaremill.macwire.*

object MacwireDemo extends ZIOAppDefault {

  lazy val connectionPool = wire[ConnectionPool]
  lazy val dbClient = wire[DbClient]
  lazy val customerRepo = wire[CustomerRepository]
  lazy val logger = wire[Logger]
  lazy val emailClient = wire[EmailClient]
  lazy val advertisementService = wire[AdvertisementService]
  lazy val customerService = wire[CustomerService]
  lazy val api = wire[CashierApi]

  def run =
    api.processLoyaltyCard("12345")

  class CashierApi(
      customerService: CustomerService,
      advertisementService: AdvertisementService,
      emailClient: EmailClient
  ) {

    def processLoyaltyCard(cardId: String): Task[Unit] =
      for {
        customer <- customerService
          .findCustomerByCardId(cardId)
          .someOrFail(CustomerNotFound(cardId))
        coupons <- advertisementService.findCouponsFor(customer.id)
        _ <- emailClient.sendCoupons(customer.email, coupons)
      } yield ()
  }

  class DbClient(connectoonPool: ConnectionPool)

  class CustomerRepository(db: DbClient) {
    def findById(id: String): ZIO[Any, Nothing, Option[Customer]] =
      ZIO.some(Customer(id, "first_name", "name@email"))
  }

  class CustomerService(customerRepo: CustomerRepository, logger: Logger) {
    def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]] =
      customerRepo.findById(id)
  }

  class Logger() {
    def log(s: String): Task[Unit] = ZIO.log(s)
  }

  class EmailClient(logger: Logger) {
    def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] =
      logger.log(
        s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
      )
  }

  class AdvertisementService() {
    def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
      ZIO.succeed(
        Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
      )
  }

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  class ConnectionPool() {
    def close: Unit = ()
  }
  object ConnectionPool {
    def create(): ConnectionPool = new ConnectionPool()
  }

  case class CustomerNotFound(id: String) extends Throwable
}
