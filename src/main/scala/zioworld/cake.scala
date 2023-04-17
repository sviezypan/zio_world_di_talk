package zioworld

import zio.*

object CakeExample extends ZIOAppDefault:

  def run = ObjectRegistry.cashierApi.processLoyaltyCard("12345")

  trait CustomerRepositoryComponent:
    val customerRepository: CustomerRepository

    class CustomerRepository():
      def findById(id: String): ZIO[Any, Nothing, Option[Customer]] =
        ZIO.some(Customer(id, "first_name", "name@email"))


  trait LoggerComponent:
    val logger: Logger

    class Logger():
      def log(s: String): Task[Unit] = ZIO.log(s)
    

  trait CustomerServiceComponent:
    this: CustomerRepositoryComponent =>

    val customerService: CustomerService

    class CustomerService:
      def findCustomerByCardId(
          id: String
      ): ZIO[Any, Nothing, Option[Customer]] =
        customerRepository.findById(id)
    

  trait AdvertisementServiceComponent:
    val advertisementService: AdvertisementService

    class AdvertisementService():
      def findCouponsFor(id: String): ZIO[Any, Nothing, Seq[Coupon]] =
        ZIO.succeed(
          Seq(Coupon("Coca cola buy 1 get 1 free"), Coupon("Beer discount"))
        )
    
  

  trait EmailClientComponent:
    this: LoggerComponent =>

    val emailClient: EmailClient

    class EmailClient():
      def sendCoupons(to: String, coupons: Seq[Coupon]): Task[Unit] =
        logger.log(
          s"sending email to ${to} with content ${coupons.map(_.title).mkString(" & ")}"
        )
    
  

  trait CashierApiComponent:
    this: CustomerServiceComponent
      with AdvertisementServiceComponent
      with EmailClientComponent
      with LoggerComponent
      with CustomerRepositoryComponent =>
    val cashierApi: CashierApi

    class CashierApi:
      def processLoyaltyCard(cardId: String): Task[Unit] =
        for {
          customer <- customerService
            .findCustomerByCardId(cardId)
            .someOrFail(CustomerNotFound(cardId))
          coupons <- advertisementService.findCouponsFor(customer.id)
          _ <- emailClient.sendCoupons(customer.email, coupons)
        } yield ()
    
  

  object ObjectRegistry
      extends CashierApiComponent
      with CustomerServiceComponent
      with CustomerRepositoryComponent
      with EmailClientComponent
      with LoggerComponent
      with AdvertisementServiceComponent:
    val cashierApi: CashierApi = new CashierApi()
    val customerService: CustomerService = new CustomerService()
    val emailClient: EmailClient = new EmailClient()
    val logger: Logger = new Logger()
    val advertisementService: AdvertisementService = new AdvertisementService()
    val customerRepository: CustomerRepository = new CustomerRepository()
  

  case class Customer(id: String, name: String, email: String)
  case class Coupon(title: String)

  case class CustomerNotFound(id: String) extends Throwable
