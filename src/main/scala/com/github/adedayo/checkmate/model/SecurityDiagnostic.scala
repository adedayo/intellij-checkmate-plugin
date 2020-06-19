package com.github.adedayo.checkmate.model


case class SecurityDiagnostic(justification: Justification, range: Range,
                              highlightRange: Range, source: String, location: String,
                              providerID: String, excluded: Boolean)

case class Range(start: Position, end: Position)

case class Position(line: Int, character: Int)

case class Justification(headline: Evidence, reasons: List[Evidence])

case class Evidence(description: String, confidence: String)


case class SensitiveFile(extension: String, description: String, excluded: Boolean)