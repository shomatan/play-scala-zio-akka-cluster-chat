package interfaces.runtime

import controllers.AssetsComponents
import interfaces.controllers.{ ConversationController, HomeController, SendCommentController }
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.slick.SlickComponents
import play.api.http.{ DefaultHttpErrorHandler, HtmlOrJsonHttpErrorHandler, JsonHttpErrorHandler }
import play.filters.HttpFiltersComponents
import router.Routes
import zio.Runtime
import zio.internal.Platform

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with SlickComponents
    with HttpFiltersComponents
    with AssetsComponents {
  override lazy val httpErrorHandler =
    new HtmlOrJsonHttpErrorHandler(
      new DefaultHttpErrorHandler(environment, configuration, devContext.map(_.sourceMapper), Some(router)),
      new JsonHttpErrorHandler(environment, devContext.map(_.sourceMapper))
    )

  val layers = new AppLayers(this)

  val runtime = Runtime.unsafeFromLayer(
    layers.controllerProviders,
    Platform.fromExecutionContext(executionContext)
  )

  val router = new Routes(
    httpErrorHandler,
    new HomeController(controllerComponents),
    runtime.environment.get[SendCommentController.Provider].apply(runtime),
    runtime.environment.get[ConversationController.Provider].apply(runtime),
    assets
  )
}
