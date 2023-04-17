package zioworld

import zio.*

object EnvironmentOveruseDemo extends ZIOAppDefault {

  def run = CashierApi
    .processLoyaltyCard("12345")
    .provide(
      CustomerRepository.layer,
      Logger.layer,
      AdvertisementRepository.layer,
      EmailSender.layer,
      DbClient.layer
    )

  object CashierApi {

    def processLoyaltyCard(cardId: String): ZIO[
      CustomerRepository
        with Logger
        with AdvertisementRepository
        with EmailSender
        with DbClient,
      Throwable,
      Unit
    ] =
      for {
        customer <- CustomerService
          .findCustomerByCardId(cardId)
          .someOrFail(CustomerNotFound(cardId))
        coupons <- AdvertisementService.findCouponsFor(customer.id)
        _ <- EmailClient.sendCoupons(customer.email, coupons)
      } yield ()
  }

  class DbClient(connectoonPool: ConnectionPool)

  object DbClient:
    def layer =
      ZLayer.scoped {
        for
          cp <- ZIO.acquireRelease {
            ZIO.succeed(ConnectionPool.create())
          } { cp =>
            ZIO.succeed(cp.close)
          }
        yield new DbClient(cp)
      }

  case class CustomerRepository(db: DbClient):
    def findById(id: String): ZIO[DbClient, Nothing, Option[Customer]] =
      ZIO.some(Customer(id, "first_name", "name@email"))

  object CustomerRepository:
    def layer = ZLayer.fromFunction(CustomerRepository.apply _)

  object CustomerService:
    def findCustomerByCardId(
        id: String
    ): ZIO[CustomerRepository with DbClient, Nothing, Option[Customer]] =
      for
        customerRepo <- ZIO.service[CustomerRepository]
        customer <- customerRepo.findById(id)
      yield (customer)

  class Logger():
    def log(s: String): Task[Unit] = ZIO.log(s)

  object Logger:
    def layer = ZLayer.succeed(new Logger())

  // not used, just for demonstration purposes
  trait EmailSender
  trait AdvertisementRepository

  object EmailSender:
    def layer = ZLayer.succeed(new EmailSender {})

  object AdvertisementRepository:
    def layer = ZLayer.succeed(new AdvertisementRepository {})

  object EmailClient:
    def sendCoupons(
        to: String,
        coupons: Seq[Coupon]
    ): RIO[EmailSender with Logger, Unit] =
      for {
        logger <- ZIO.service[Logger]
        _ <- logger.log(
          s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
        )
      } yield ()

  object AdvertisementService:
    def findCouponsFor(
        id: String
    ): ZIO[AdvertisementRepository, Nothing, Seq[Coupon]] =
      ZIO.succeed(
        Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
      )

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  class ConnectionPool():
    def close: Unit = ()

  object ConnectionPool:
    def create(): ConnectionPool = new ConnectionPool()

  case class CustomerNotFound(id: String) extends Throwable
}

// example commented out because `Has` available only in ZIO 1
object ServicePattern1Demo extends ZIOAppDefault {
  // type CashierApi = Has[Service]

  // trait Service {
  //     def processLoyaltyCard(cardId: String): Task[Unit]
  // }
  // object Service {
  //     final class Live(
  //         customerService: CustomerService,
  //         advertisementService: AdvertisementService,
  //         emailClient: EmailClient) extends Service {

  //         def processLoyaltyCard(cardId: String): Task[Unit] =
  //             for {
  //                 customer <- customerService.findCustomerByCardId(cardId)
  //                     .someOrFail(CustomerNotFound(cardId))
  //                 coupons  <- advertisementService.findCouponsFor(customer.id)
  //                 _        <- emailClient.sendCoupons(customer.email, coupons)
  //             } yield ()
  //     }
  // }

  // val live: URLayer[Has[AdvertisementService] with Has[CustomerService] with Has[EmailClient], CashierApi] =
  //     ZLayer.fromServices[AdvertisementService, CustomerService, EmailClient](new Service.Live(_, _, _))

  // def processLoyaltyCard(cardId: String): RIO[CashierApi, Unit] =
  //     ZIO.accessM(_.get.processLoyaltyCard(cardId))

  def run = ???
}
