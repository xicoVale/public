/* Generated By:JJTree: Do not edit this line. ASTCompound.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.cs3.prolog.internal.cterm.parser;

public
class ASTCompound extends org.cs3.prolog.internal.cterm.parser.ASTNode {
  public ASTCompound(int id) {
    super(id);
  }

  public ASTCompound(CanonicalTermParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  @Override
public Object jjtAccept(CanonicalTermParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=9c8aac8c819fc611a01be69c7fdc7c39 (do not edit this line) */