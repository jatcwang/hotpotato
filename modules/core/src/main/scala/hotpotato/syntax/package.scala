package hotpotato.syntax

import hotpotato.ErrorTransSyntax

object all extends SyntaxAll

object embedder extends EmbedderIdSyntax
object errortrans extends ErrorTransEmbedSyntax with ErrorTransSyntax

trait SyntaxAll extends ErrorTransEmbedSyntax with ErrorTransSyntax with EmbedderIdSyntax
