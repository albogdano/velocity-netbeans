/*
 * $Id: VelocityAnalyser.java 21 2011-04-26 00:51:48Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.jcclexer.node;

/**
 * Visitor base Implementation that servers as base class for concrete
 * analyzers.
 *
 * <p>
 *    Each {@code visit} method implementation takes no actions beside
 *    traversing the child nodes.
 * </p>
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
abstract public class VelocityAnalyser implements VelocityParserVisitor
{
   /**
    * Creates new {@code VelocityAnalyser}.
    */
   public VelocityAnalyser()
   {
   }

   @Override public Object visit(final SimpleNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTprocess node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTEscapedDirective node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTEscape node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTComment node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTUnparsedContent node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTFloatingPointLiteral node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTIntegerLiteral node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTStringLiteral node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTIdentifier node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTWord node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTDirective node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTBlock node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTMap node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTObjectArray node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTIntegerRange node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTMethod node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTReference node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTTrue node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTFalse node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTText node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTForEachStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTMacroStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTIncludeStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTIfStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTElseStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTElseIfStatement node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTSetDirective node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTStop node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTExpression node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTAssignment node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTOrNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTAndNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTEQNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTNENode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTLTNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTGTNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTLENode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTGENode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTAddNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTSubtractNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTMulNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTDivNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTModNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   @Override public Object visit(final ASTNotNode node, final Object oData)
   {
      return(node.childrenAccept(this, oData));
   }

   abstract public void openTransaction();

   abstract public void commitTransaction();
}
