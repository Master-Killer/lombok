/*
 * Copyright (C) 2015 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.javac.handlers.singulars;

import static lombok.javac.handlers.JavacHandlerUtil.*;

import org.mangosdk.spi.ProviderFor;

import lombok.core.LombokImmutableList;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.javac.handlers.JavacSingularsRecipes.JavacSingularizer;
import lombok.javac.handlers.JavacSingularsRecipes.SingularData;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

@ProviderFor(JavacSingularizer.class)
public class JavacJavaUtilListSingularizer extends JavacJavaUtilListSetSingularizer {
	@Override public LombokImmutableList<String> getSupportedTypes() {
		return LombokImmutableList.of("java.util.List", "java.util.Collection", "java.lang.Iterable");
	}
	
	@Override public void appendBuildCode(SingularData data, JavacNode builderType, JCTree source, ListBuffer<JCStatement> statements, Name targetVariableName) {
		if (useGuavaInstead(builderType)) {
			guavaListSetSingularizer.appendBuildCode(data, builderType, source, statements, targetVariableName);
			return;
		}
		
		JavacTreeMaker maker = builderType.getTreeMaker();
		List<JCExpression> jceBlank = List.nil();

		JCStatement defStats = createMutableListCopy(maker, data, builderType, source);
		
		JCExpression localShadowerType = chainDotsString(builderType, data.getTargetFqn());
		localShadowerType = addTypeArgs(1, false, builderType, localShadowerType, data.getTypeArgs(), source);
		JCStatement varDefStat = maker.VarDef(maker.Modifiers(0), data.getPluralName(), localShadowerType, null);
		statements.append(varDefStat);
		statements.append(defStats);
	}

	private List<JCStatement> createListCopy(JavacTreeMaker maker, SingularData data, JavacNode builderType, JCTree source) {
		List<JCExpression> jceBlank = List.nil();

		JCExpression argToUnmodifiable = createMutableListCopyExpression(maker, data, builderType, source, jceBlank);

		JCStatement unmodifiableStat; {
			// pluralname = Collections.unmodifiableInterfaceType(-newlist-);
			JCExpression invoke = maker.Apply(jceBlank, chainDots(builderType, "java", "util", "Collections", "unmodifiableList"), List.of(argToUnmodifiable));
			unmodifiableStat = maker.Exec(maker.Assign(maker.Ident(data.getPluralName()), invoke));
		}

		return List.of(unmodifiableStat);
	}

	private JCStatement createMutableListCopy(JavacTreeMaker maker, SingularData data, JavacNode builderType, JCTree source) {
		List<JCExpression> jceBlank = List.nil();

		JCExpression argToUnmodifiable = createMutableListCopyExpression(maker, data, builderType, source, jceBlank);

		return maker.Exec(maker.Assign(maker.Ident(data.getPluralName()), argToUnmodifiable));
	}

	private JCExpression createMutableListCopyExpression(JavacTreeMaker maker, SingularData data, JavacNode builderType, JCTree source, List<JCExpression> jceBlank) {
		Name thisName = builderType.toName("this");
		// new java.util.ArrayList<Generics>(this.pluralName);
		List<JCExpression> constructorArgs = List.nil();
		JCExpression thisDotPluralName = maker.Select(maker.Ident(thisName), data.getPluralName());
		constructorArgs = List.<JCExpression>of(thisDotPluralName);
		JCExpression targetTypeExpr = chainDots(builderType, "java", "util", "ArrayList");
		targetTypeExpr = addTypeArgs(1, false, builderType, targetTypeExpr, data.getTypeArgs(), source);
		return maker.NewClass(null, jceBlank, targetTypeExpr, constructorArgs, null);
	}
}
