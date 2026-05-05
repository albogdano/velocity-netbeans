/*
 * $Id: Directive.java 2 2009-12-18 01:20:47Z werner $
 *
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.jcclexer;

/**
 * Base class for all directives used in Velocity.
 *
 * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
 */
public class Directive
{
   /** Block directive indicator */
   public static final int BLOCK = 1;

   /** Line directive indicator */
   public static final int LINE = 2;
   
   private final int m_iType;

   /**
    * Creates new {@code Directive}.
    */
   public Directive(final int iType)
   {
      m_iType = iType;
   }

   /**
    * Get the directive type BLOCK/LINE.
    *
    * @return The directive type BLOCK/LINE.
    */
   public int getType()
   {
      return(m_iType);
   }
}
