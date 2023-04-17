package zioworld

import zio.*
import com.google.inject.*
import net.codingwell.scalaguice.ScalaModule
import net.codingwell.scalaguice.InjectorExtensions.ScalaInjector

object GuiceDemo extends ZIOAppDefault {

  class MyModule extends AbstractModule with ScalaModule

  def run = {
    val injector = Guice.createInjector(new MyModule())
    val api = injector.instance[CashierApi]
    api.processLoyaltyCard("12345")
  }

  class CashierApi @Inject() (
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

  class ConnectionPool @Inject() () {
    def close(): Unit = ()
  }

  class DbClient @Inject() (connectoonPool: ConnectionPool)

  class CustomerRepository @Inject() (db: DbClient) {
    def findById(id: String): ZIO[Any, Nothing, Option[Customer]] =
      ZIO.some(Customer(id, "first_name", "name@email"))
  }

  class CustomerService @Inject() (
      customerRepo: CustomerRepository,
      logger: Logger
  ) {
    def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]] =
      customerRepo.findById(id)
  }

  class Logger @Inject() () {
    def log(s: String): Task[Unit] = ZIO.log(s)
  }

  class EmailClient @Inject() (logger: Logger) {
    def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] =
      logger.log(
        s"Sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
      )
  }

  class AdvertisementService @Inject() () {
    def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
      ZIO.succeed(
        Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
      )
  }

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  object ConnectionPool {
    def create(): ConnectionPool = new ConnectionPool()
  }

  case class CustomerNotFound(id: String) extends Throwable
}
