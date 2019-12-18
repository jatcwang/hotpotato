package hotpotato
import java.util.UUID

import zio._
import shapeless._

import collection.mutable

object ServiceExample {

  final case class NotFound[IdType](id: IdType)
  final case class NotAuthorized()
  final case class InvalidCreationData(message: String)

  final case class UserId(value: UUID) extends AnyVal
  final case class User(id: UserId, name: String, role: Role)
  final case class UserCreateData(name: String, role: Role)

  final case class DocumentId(value: UUID) extends AnyVal
  final case class CreateDocumentData(content: String)
  final case class Document(id: DocumentId, owner: UserId, content: String)

  final case class RequestContext(userId: UserId)

  sealed trait Role
  final case object Creator extends Role
  final case object Reader extends Role

  trait UserService {
    def findUser(userId: UserId): IO[NotFound[UserId] :+: CNil, User]
    def createUser(data: UserCreateData): IO[NotAuthorized :+: InvalidCreationData :+: CNil, UserId]
  }

  trait DocumentService {
    def findDocument(docId: DocumentId)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: NotFound[DocumentId] :+: CNil, Document]
    def createDocument(createData: CreateDocumentData)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: InvalidCreationData :+: CNil, DocumentId]
    def deleteDocument(docId: DocumentId)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: NotFound[DocumentId] :+: CNil, Unit]
  }

  class DocumentServiceImpl(userService: UserService) extends DocumentService {
    val ADMIN_ID  = UserId(UUID.fromString("0008d4c5-2211-4e02-867d-f9debadff535"))
    val documents = mutable.ArrayBuffer.empty[Document]

    override def findDocument(docId: DocumentId)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: NotFound[DocumentId] :+: CNil, Document] = {
      implicit val embedder: Embedder[NotAuthorized :+: NotFound[DocumentId] :+: CNil] =
        Embedder.make
      documents.find(_.id == docId) match {
        case Some(doc) =>
          if (doc.owner == ctx.userId) {
            IO.succeed(doc)
          } else IO.fail(NotAuthorized().embed)
        case None => IO.fail(NotFound(docId).embed)
      }
    }

    override def createDocument(createData: CreateDocumentData)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: InvalidCreationData :+: CNil, DocumentId] = {
      implicit val embedder: Embedder[NotAuthorized :+: InvalidCreationData :+: CNil] =
        Embedder.make
      for {
        user <- userService.findUser(ctx.userId).mapErrorAllInto(_ => NotAuthorized().embed)
        docId <- if (user.role == Creator) {
                  IO.effectTotal {
                    val doc =
                      Document(DocumentId(UUID.randomUUID()), ctx.userId, createData.content)
                    documents += doc
                    doc.id
                  }
                } else {
                  IO.fail(NotAuthorized().embed)
                }
      } yield docId
    }

    override def deleteDocument(docId: DocumentId)(
      ctx: RequestContext,
    ): IO[NotAuthorized :+: NotFound[DocumentId] :+: CNil, Unit] = {
      implicit val embedder: Embedder[NotAuthorized :+: NotFound[DocumentId] :+: CNil] =
        Embedder.make
      IO.effectSuspendTotal {
        documents.find(_.id == docId) match {
          case Some(doc) =>
            if (doc.owner == ctx.userId) {
              IO.succeed(())
            } else {
              IO.fail(NotAuthorized().embed)
            }
          case None => IO.fail(NotFound(docId).embed)
        }
      }
    }
  }

}
