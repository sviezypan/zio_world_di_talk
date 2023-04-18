package zioworld

import zio.*
import cats.data.Reader
import zioworld.EnvironmentOveruseDemo.AdvertisementRepository

object ReaderMonad2Example {

  // If we want to use multiple readers in a for-comprehension, the argument types will need to be the same

  val dependencyWrapper: DependencyWrapper = ???

  def run = CashierApi.processLoyaltyCard("12345")
                   .run(dependencyWrapper)

  case class DependencyWrapper(customerRepoWrapper: CustomerRepoWrapper, 
                               advertisementRepo: AdvertisementRepository, 
                               logger: Logger)

  case class CustomerRepoWrapper(customerRepo: CustomerRepository, 
                                 dbClient: DbClient)

  def runAlt = CashierApi.processLoyaltyCardAlt("12345").run(Dependencies)

  object Dependencies extends DbClient(ConnectionPool.create()) 
    with CustomerRepository with AdvertisementRepository with Logger

  object CashierApi {
    def processLoyaltyCard(cardId: String): Reader[DependencyWrapper, Coupon] =
      for {
          customer <- CustomerService.findCustomerByCardId(cardId)
                          .local[DependencyWrapper](_.customerRepoWrapper)
          coupon   <- AdvertisementService.findCouponFor(customer.get.id)
                          .local[DependencyWrapper](_.advertisementRepo)
          _        <- EmailClient.sendCoupon(customer.get.email, coupon)
                          .local[DependencyWrapper](_.logger)
      } yield coupon

    def processLoyaltyCardAlt(cardId: String): Reader[
      CustomerRepository & DbClient & AdvertisementRepository & Logger,
      Coupon
    ] = 
      (for {
        customer <- CustomerService.findCustomerByCardIdAlt(cardId)
        coupon   <- AdvertisementService.findCouponFor(customer.get.id)
        _        <- EmailClient.sendCoupon(customer.get.email, coupon)
      } yield coupon)
  }

  object CustomerService {
    def findCustomerByCardId(
        id: String
    ): Reader[CustomerRepoWrapper, Option[Customer]] =
    Reader { (wrapper: CustomerRepoWrapper) =>
        wrapper.customerRepo.findById(id).run(wrapper.dbClient)
    }

    def findCustomerByCardIdAlt(
        id: String
    ): Reader[CustomerRepository & DbClient, Option[Customer]] =
      Reader { (customerRepo: CustomerRepository & DbClient) =>
          customerRepo.findById(id).run(customerRepo)
      }
  }

  case class Price(amount: Int, currency: String)
  case class Product(name: String, price: Price)

  class DiscountService {
    def discountProduct(product: Product, discountCode: String): Price =
      println("discounting price")
      Price(100, "EUR")

  }

  class DbClient(connectoonPool: ConnectionPool)

  trait CustomerRepository {
    def findById(id: String): Reader[DbClient, Option[Customer]] =
      Reader { (dbClient: DbClient) =>
        Some(Customer(id, "first_name", "name@email"))
      }
  }

  trait Logger {
    def log(s: String): Task[Unit] = ZIO.log(s)
  }

  object EmailClient {
    def sendCoupon(to: String, coupon: Coupon): Reader[Logger, Unit] =
      Reader { (logger: Logger) =>
        logger.log(
          s"sending email to ${to} with content ${coupon.title.mkString(" & ")}"
        )
      }
  }

  trait AdvertisementRepository {
    def findCouponFor(id: String): Coupon = ???
  }

  object AdvertisementService {
    def findCouponFor(
        id: String
    ): Reader[AdvertisementRepository, Coupon] =
      Reader { (advertisementRepo: AdvertisementRepository) =>
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

}