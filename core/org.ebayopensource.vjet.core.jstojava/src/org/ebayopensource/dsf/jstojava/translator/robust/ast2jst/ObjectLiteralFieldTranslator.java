/*******************************************************************************
 * Copyright (c) 2005-2011 eBay Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.ebayopensource.dsf.jstojava.translator.robust.ast2jst;

import java.util.List;

import org.ebayopensource.dsf.jsgen.shared.ids.ScopeIds;
import org.ebayopensource.dsf.jst.IJstMethod;
import org.ebayopensource.dsf.jst.IJstType;
import org.ebayopensource.dsf.jst.JstSource;
import org.ebayopensource.dsf.jst.declaration.JstFuncType;
import org.ebayopensource.dsf.jst.expr.FuncExpr;
import org.ebayopensource.dsf.jst.term.JstIdentifier;
import org.ebayopensource.dsf.jst.term.NV;
import org.ebayopensource.dsf.jst.token.IExpr;
import org.ebayopensource.dsf.jstojava.parser.comments.IJsCommentMeta;
import org.ebayopensource.dsf.jstojava.translator.TranslateHelper;
import org.ebayopensource.dsf.jstojava.translator.robust.completion.JstCommentUtil;
import org.ebayopensource.dsf.jstojava.translator.robust.completion.JstCompletion;
import org.ebayopensource.dsf.jstojava.translator.robust.completion.JstFieldOrMethodCompletion;
import org.eclipse.mod.wst.jsdt.core.ast.IExpression;
import org.eclipse.mod.wst.jsdt.internal.compiler.ast.ASTNode;
import org.eclipse.mod.wst.jsdt.internal.compiler.ast.ObjectLiteralField;
import org.eclipse.mod.wst.jsdt.internal.compiler.ast.SingleNameReference;

public class ObjectLiteralFieldTranslator extends
		BaseAst2JstTranslator<ObjectLiteralField, Object> {
	
	private NV m_result;

	@Override
	protected Object doTranslate(ObjectLiteralField astObjectliteralField) {
		// store completion position
		int completionPos = m_ctx.getCompletionPos();
		try {
			final IExpr value = (IExpr)getTranslatorAndTranslate(astObjectliteralField.initializer);
			final JstIdentifier id = createId(astObjectliteralField); 
			bindObjLiteralId(astObjectliteralField, id, value);
			
			NV nv = new NV(id, value); 
			int start = id.getSource().getStartOffSet();
			if (value.getSource() != null) {
				int end = value.getSource().getEndOffSet();
				int length = end - start;
				nv.setSource(TranslateHelper.createJstSource(m_ctx
						.getSourceUtil(), length, start, end));
			}
			m_result = nv;
			return nv; 
		} finally {
			// restore previous completion position
			m_ctx.setCompletionPos(completionPos);
		}			
	}

	private void bindObjLiteralId(ObjectLiteralField astObjectliteralField,
			final JstIdentifier id, IExpr value) {
		// added by huzhou@ebay.com to bind objectLiteral's methods early on
		if (value instanceof FuncExpr) {
			final TranslateHelper.RenameableSynthJstProxyMethod mtdBinding = new TranslateHelper.RenameableSynthJstProxyMethod(((FuncExpr)value).getFunc(), id.getName());
			id.setJstBinding(mtdBinding);
			id.setType(new JstFuncType(mtdBinding));
		}

		final List<IJsCommentMeta> metaArr = getCommentMeta(astObjectliteralField);
		if (metaArr != null && metaArr.size() > 0) {
			final IJsCommentMeta meta = metaArr.get(0);
			final IJstType metaDefinedType = TranslateHelper.findType(m_ctx,
					meta.getTyping(), meta);
			if (metaDefinedType != null) {
				id.setType(metaDefinedType);
				if(metaDefinedType instanceof JstFuncType){
					final IJstMethod replacement = TranslateHelper.MethodTranslateHelper.createJstSynthesizedMethod(metaArr, m_ctx, id.getName());
					if(replacement != null){
						TranslateHelper.replaceSynthesizedMethodBinding(id, replacement);
					}
				}
			}
		}
	}
	
	private List<IJsCommentMeta> getCommentMeta(
			ObjectLiteralField ast) {
		final int next = m_ctx.getNextNodeSourceStart();
		return m_ctx.getCommentCollector().getCommentMeta(
				ast.sourceStart, 
				m_ctx.getPreviousNodeSourceEnd(), next);
	}

	private JstIdentifier createId(ObjectLiteralField astObjectliteralField) {
		JstIdentifier id = new JstIdentifier(astObjectliteralField.getFieldName().toString());
		int startOffset = astObjectliteralField.sourceStart;
		int endOffset = astObjectliteralField.sourceEnd;
		int length = endOffset - startOffset + 1;
		id.setSource(TranslateHelper.createJstSource(m_ctx.getSourceUtil(), length,startOffset,endOffset));
		return id;
	}

	@Override
	protected void checkForCompletion(ObjectLiteralField astNode) {
		if (m_ctx.isCreatedCompletion()) {
			return;
		}
		
		int startPos = astNode.sourceStart();
		int endPos = getSourceEnd(astNode);

		int completionPos = m_ctx.getCompletionPos();
		boolean insideSource = completionPos >= startPos
				&& completionPos <= endPos;

		boolean isAfterSource = completionPos == endPos + 1;
		if(!isAfterSource && astNode.getInitializer() instanceof SingleNameReference){
			final SingleNameReference missing = (SingleNameReference)astNode.getInitializer();
			if(TranslateHelper.MISSING_TOKEN.equals(String.valueOf(missing.getToken()))){
				isAfterSource = true;
			}
		}

		if (insideSource || isAfterSource) {
			JstCompletion completion = createCompletion(astNode, isAfterSource);
			if (completion != null) {
				JstCommentUtil.fillCompletion((ASTNode) astNode, m_ctx, completion);
//				m_ctx.getS
				m_ctx.addSyntaxError(completion);
			}
		}
	}
	
	@Override
	protected JstCompletion createCompletion(
			ObjectLiteralField astObjectLiteralField, boolean isAfterSource) {

			int completionPos = m_ctx.getCompletionPos();
			if(completionPos < astObjectLiteralField.sourceStart){
				return null;
			}
			
			String preStr = new String(m_ctx.getOriginalSource(),
					astObjectLiteralField.sourceStart, completionPos
							- astObjectLiteralField.sourceStart);
			if (preStr == null) {
				return null;
			}
			String[] strs = (" " + preStr + " ").split(":");
			if (strs.length == 0) {
				// TODO
				return null;
			} else if (strs.length == 1) {
				String token = strs[0].trim();
				JstCompletion completion = new JstFieldOrMethodCompletion(m_result,
						m_ctx.getCurrentScope() == ScopeIds.PROPS);
				IExpression expr = astObjectLiteralField.getFieldName();
				JstSource jstSource = null;
				if (expr != null) {
					if (expr.sourceEnd() + 1 > completionPos) {
						jstSource = createSource(
								expr.sourceStart(),
								expr.sourceEnd() + 1, m_ctx
										.getSourceUtil());
					} else {
						jstSource = createSource(
								expr.sourceStart(),
								completionPos, m_ctx
										.getSourceUtil());
					}
				}
				completion.setSource(jstSource);
				completion.setCompositeToken(preStr);
				completion.setToken(token);
				m_ctx.setCreatedCompletion(true);
				completion.setScopeStack(m_ctx.getScopeStack());
				return completion;
			} else if (strs.length == 2) {
				String token = strs[1].trim();
				//if cursor is After "(", null will be return
				if(token.indexOf("(") >= 0 || !isJavaIdentifier(token)) {
					return null;
				}
				JstCompletion completion = new JstFieldOrMethodCompletion(m_result, ScopeIds.PROPS == m_ctx.getCurrentScope());
				completion.setCompositeToken(preStr);
				completion.setToken(token);
				m_ctx.setCreatedCompletion(true);
				completion.setScopeStack(m_ctx.getScopeStack());
				return completion;
			}

//		}
		return null;
	}
}
