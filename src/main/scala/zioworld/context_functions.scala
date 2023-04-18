package zioworld

import zio.*

object ContextFunctionsDemo extends ZIOAppDefault {

  def run =
    given customerRepository: CustomerRepository = CustomerRepository()
    given advertisementRepository: AdvertisementRepository = AdvertisementRepository()
    given logger: Logger = Logger()
    given dbClient: DbClient = DbClient(ConnectionPool.create())

    CashierApi.processLoyaltyCard("12345")

  object CashierApi:
    def processLoyaltyCard(
        cardId: String
    ): CustomerRepository ?=> DbClient ?=> AdvertisementRepository ?=> Logger ?=> Task[Unit] =
      for {
        customer <- CustomerService
          .findCustomerByCardId(cardId)
          .someOrFail(CustomerNotFound(cardId))
        coupon  <- AdvertisementService.findCouponFor(customer.id)
        _       <- EmailClient.sendCoupon(customer.email, coupon)
      } yield ()

  object CustomerService:
    def findCustomerByCardId(
        id: String
    ): CustomerRepository ?=> DbClient ?=> UIO[Option[Customer]] = 
        val customerRepository = summon[CustomerRepository]
        customerRepository.findById(id)

  class DiscountService {
    def discountProduct(product: Product, discountCode: String): UIO[Price] =
      println("discounting price")
      ZIO.succeed(Price(100, "EUR"))

  }

  class DbClient(connectoonPool: ConnectionPool)

  class CustomerRepository {
    def findById(id: String): DbClient ?=> UIO[Option[Customer]] = {
        val unused = summon[DbClient]

        ZIO.succeed(Some(Customer(id, "first_name", "name@email")))
      }
  }

  class Logger {
    def log(s: String): UIO[Unit] = ZIO.log(s)
  }

  object EmailClient {
    def sendCoupon(to: String, coupon: Coupon): Logger ?=> UIO[Unit] = { 
        val logger = summon[Logger]
        logger.log(
          s"sending email to ${to} with content ${coupon.title}"
        )
      }
  }

  class AdvertisementRepository {
    def findCouponFor(id: String): UIO[Coupon] = ZIO.succeed(Coupon("buy one coca cola get one free"))
  }

  object AdvertisementService {
    def findCouponFor(
        id: String
    ): AdvertisementRepository ?=> UIO[Coupon] = {
        val advertisementRepo = summon[AdvertisementRepository]
        advertisementRepo.findCouponFor(id)
      }
  }

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  object ConnectionPool:
    def create(): ConnectionPool = new ConnectionPool()

  class ConnectionPool():
    def close: Unit = ()

  case class CustomerNotFound(id: String) extends Throwable

  case class Price(amount: Int, currency: String)
  case class Product(name: String, price: Price)

}
