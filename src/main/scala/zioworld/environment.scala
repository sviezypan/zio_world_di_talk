package zioworld

import zio.*

type UserId
type AuthToken
class InvalidSessionError extends Throwable

class UserProfile
class Request

trait UserSession {
  def userId: UserId

  def token: AuthToken

  def put(prop: String, value: String): UIO[Unit]

  def get(prop: String): UIO[String]
}

trait UserService:
  def getUserProfile: ZIO[UserSession, Nothing, UserProfile]

trait AuthService:
  def authenticate(req: Request): ZLayer[Any, InvalidSessionError, UserSession]

class UserApi(userService: UserService, authService: AuthService):
  def getUserProfile(request: Request): ZIO[Any, InvalidSessionError, UserProfile] =
    userService.getUserProfile
      .provideLayer(authService.authenticate(request))
