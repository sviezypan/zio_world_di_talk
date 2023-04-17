package zioworld

import zio.*

object ServicePatter2Demo extends ZIOAppDefault:

  def run =
    (for {
      cashierApi <- ZIO.service[CashierApi]
      _ <- cashierApi.processLoyaltyCard("12345")
    } yield ())
      .provide(
        CashierApi.layer,
        CustomerRepository.layer,
        CustomerService.layer,
        EmailClient.layer,
        Logger.layer,
        AdvertisementService.layer
      )

  trait CashierApi:
    def processLoyaltyCard(cardId: String): Task[Unit]

  case class CashierApiImpl(
      customerService: CustomerService,
      advertisementService: AdvertisementService,
      emailClient: EmailClient
  ) extends CashierApi:
    def processLoyaltyCard(cardId: String): Task[Unit] =
      for {
        customer <- customerService
          .findCustomerByCardId(cardId)
          .someOrFail(CustomerNotFound(cardId))
        coupons <- advertisementService.findCouponsFor(customer.id)
        _ <- emailClient.sendCoupons(customer.email, coupons)
      } yield ()

  object CashierApi:
    def layer =
      ZLayer.fromFunction(CashierApiImpl.apply _)

  case class ConnectionPool():
    def close: Unit = ()
    
  case class DbClient(connectoonPool: ConnectionPool)

  trait CustomerRepository:
    def findById(id: String): ZIO[Any, Nothing, Option[Customer]]

  class UserApi(userService: UserService, authService: AuthService):

    def getUserProfile(
        request: Request
    ): ZIO[Any, InvalidSessionError, UserProfile] =
      userService.getUserProfile
        .provideLayer(authService.authenticate(request))

  class CustomerRepositoryImpl() extends CustomerRepository:

    private val connectionPoolLayer =
      ZLayer.scoped {
        ZIO.acquireRelease {
          ZIO.succeed(ConnectionPool.create())
        } { connectionPool =>
          ZIO.succeed(connectionPool.close)
        }
      }

    def findById(id: String): ZIO[Any, Nothing, Option[Customer]] =
      (for {
        _ <- ZIO.service[ConnectionPool]
        customer <- ZIO.some(Customer(id, "first_name", "name@email"))
      } yield customer)
        .provideLayer(connectionPoolLayer)

  object CustomerRepository:
    def layer = ZLayer.succeed(new CustomerRepositoryImpl())

  trait CustomerService:
    def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]]

  case class CustomerServiceImpl(
      customerRepo: CustomerRepository,
      logger: Logger
  ) extends CustomerService:

    def findCustomerByCardId(id: String): ZIO[Any, Nothing, Option[Customer]] =
      customerRepo.findById(id)

  object CustomerService:
    def layer = ZLayer.fromFunction(CustomerServiceImpl.apply _)

  case class Logger():
    def log(s: String): Task[Unit] = ZIO.log(s)

  object Logger:
    def layer = ZLayer.succeed(Logger())

  case class EmailClient(logger: Logger):
    def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] =
      logger.log(
        s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
      )

  object EmailClient:
    def layer = ZLayer.fromFunction(EmailClient.apply _)

  case class AdvertisementService():
    def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
      ZIO.succeed(
        Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
      )

  object AdvertisementService:
    def layer = ZLayer.succeed(AdvertisementService())

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  object ConnectionPool:
    def create(): ConnectionPool = new ConnectionPool()

  case class CustomerNotFound(id: String) extends Throwable