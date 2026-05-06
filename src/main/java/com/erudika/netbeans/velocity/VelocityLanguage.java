///*
// * $Id: VTLDataObject.java 13 2010-01-01 05:01:13Z werner $
// *
// * Copyright (c) 2009 T-Systems International GmbH.
// * All rights reserved.
// * This software is the confidential and proprietary information
// * of T-Systems International GmbH.
// *
// */
//package com.erudika.netbeans.velocity;
//
//import com.erudika.netbeans.velocity.completion.VTLCompletionProvider;
//import com.erudika.netbeans.velocity.lexer.VTLTokenId;
//import com.erudika.netbeans.velocity.parser.VTLParser;
//import org.netbeans.api.lexer.Language;
//import org.netbeans.modules.csl.api.CodeCompletionHandler;
//import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
//import org.netbeans.modules.csl.spi.LanguageRegistration;
//import org.netbeans.modules.parsing.spi.Parser;
//import org.netbeans.modules.parsing.spi.indexing.PathRecognizerRegistration;
//import org.openide.awt.ActionID;
//import org.openide.awt.ActionReference;
//import org.openide.awt.ActionReferences;
//import org.openide.filesystems.MIMEResolver;
//import org.openide.util.NbBundle.Messages;
//
///**
// * @author <a href="mailto:werner.jaeger@t-systems.com">Werner Jaeger</a>
// */
//@LanguageRegistration(mimeType=VelocityLanguage.VTL_MIME_TYPE, useMultiview = true)
//@PathRecognizerRegistration(mimeTypes = VelocityLanguage.VTL_MIME_TYPE, libraryPathIds = {}, binaryLibraryPathIds = {}) //NOI18N
//@ActionReferences({
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
//		position = 400,
//		separatorAfter = 500
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
//		position = 300,
//		separatorAfter = 400
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
//		position = 600,
//		separatorAfter = 700
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
//		position = 1100,
//		separatorAfter = 1200
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
//		position = 100,
//		separatorAfter = 200
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
//		position = 1400,
//		separatorAfter = 1500
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
//		position = 700
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
//		position = 900,
//		separatorAfter = 1000
//	),
//	@ActionReference(
//        path = "Loaders/text/x-velocity/Actions",
//		id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
//		position = 1300,
//		separatorAfter = 1400
//	)
//})
//@Messages({"LBL_VTL_LOADER=Velocity Files"})
//@MIMEResolver.ExtensionRegistration(
//		displayName = "#LBL_VTL_LOADER",
//		mimeType = VelocityLanguage.VTL_MIME_TYPE,
//		extension = {"vm", "vsl"})
//public class VelocityLanguage extends DefaultLanguageConfig {
//
//	private static final String LINE_COMMENT_PREFIX = "#";//NOI18N
//	public static final String VTL_MIME_TYPE = "text/x-velocity";//NOI18N
//
////	@MultiViewElement.Registration(
////		displayName="#LBL_TSEditorTab",
////        iconBase="net/dfranek/typoscript/resources/ts_file_16.png",
////        persistenceType=TopComponent.PERSISTENCE_ONLY_OPENED,
////        preferredID="ts.source",
////        mimeType=TS_MIME_TYPE,
////		position=1
////	)
////    public static MultiViewEditorElement createMultiViewEditorElement(Lookup context) {
////        return new MultiViewEditorElement(context);
////    }
//
//
//	@Override
//	public Language<VTLTokenId> getLexerLanguage() {
//		return VTLTokenId.getLanguage();
//	}
//
//	@Override
//	public String getDisplayName() {
//		return "Velocity";
//	}
//
//	@Override
//	public String getLineCommentPrefix() {
//		return LINE_COMMENT_PREFIX;
//	}
//
//	@Override
//	public String getPreferredExtension() {
//		return "vm";
//	}
//
//	@Override
//	public boolean isIdentifierChar(char c) {
//		return Character.isJavaIdentifierPart(c);
//	}
//
//	@Override
//	public CodeCompletionHandler getCompletionHandler() {
//		return new VTLCompletionProvider();
//	}
//
//	@Override
//	public Parser getParser() {
//		return new VTLParser();
//	}
//
//
//
//}
