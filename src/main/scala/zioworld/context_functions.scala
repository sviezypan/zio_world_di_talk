package zioworld

import zio.*
import scala.collection.mutable.ArrayBuffer

object ContextFunctionsDemo extends ZIOAppDefault:

    def run =
        val dbClient = new DbClient(ConnectionPool.create())
        val customerRepo = new CustomerRepository(dbClient)
        val logger = new Logger()

        given customerService: CustomerService = new CustomerService(customerRepo, logger)
        given advertisementService: AdvertisementService = new AdvertisementService()
        given emailClient: EmailClient = new EmailClient(logger)

        CashierApi.processLoyaltyCard("12345")

    object CashierApi:
        def processLoyaltyCard(cardId: String): CustomerService ?=> AdvertisementService ?=> EmailClient ?=> Task[Unit] = 
            val customerService = summon[CustomerService]
            val advertisementService = summon[AdvertisementService]
            val emailClient = summon[EmailClient]
            for {
                customer <- customerService.findCustomerByCardId(cardId)
                    .someOrFail(CustomerNotFound(cardId))
                coupons  <- advertisementService.findCouponsFor(customer.id)
                _        <- emailClient.sendCoupons(customer.email, coupons)
            } yield ()
    


    class ConnectionPool():
      def close: Unit = ???

    class DbClient(connectoonPool: ConnectionPool)

    class CustomerRepository(db: DbClient):
        def findById(id: String): ZIO[Any, Nothing, Option[Customer]] = ZIO.some(Customer(id, "first_name", "name@email"))

    class CustomerService(customerRepo: CustomerRepository, logger: Logger):
        def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]] = 
            customerRepo.findById(id)

    class Logger():
        def log(s: String) : Task[Unit] = ZIO.log(s)

    class EmailClient(logger: Logger):
        def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] = 
            logger.log(s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}")

    class AdvertisementService():
        def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
            ZIO.succeed(Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount")))

    case class Customer(id: String, name: String, email: String)
    case class Coupon(title: String)

    object ConnectionPool:
        def create(): ConnectionPool = new ConnectionPool()

    case class CustomerNotFound(id: String) extends Throwable