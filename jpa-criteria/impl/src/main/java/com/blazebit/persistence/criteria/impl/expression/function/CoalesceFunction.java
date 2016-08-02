package com.blazebit.persistence.criteria.impl.expression.function;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder.Coalesce;
import javax.persistence.criteria.Expression;

import com.blazebit.persistence.criteria.impl.BlazeCriteriaBuilderImpl;
import com.blazebit.persistence.criteria.impl.ParameterVisitor;
import com.blazebit.persistence.criteria.impl.RenderContext;
import com.blazebit.persistence.criteria.impl.expression.AbstractExpression;
import com.blazebit.persistence.criteria.impl.expression.LiteralExpression;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class CoalesceFunction<T> extends AbstractExpression<T> implements Coalesce<T> {

    private static final long serialVersionUID = 1L;
    
    private final List<Expression<? extends T>> expressions;
    private Class<T> javaType;

    public CoalesceFunction(BlazeCriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
        super(criteriaBuilder, javaType);
        this.javaType = javaType;
        this.expressions = new ArrayList<Expression<? extends T>>();
    }

    @Override
    public Class<T> getJavaType() {
        return javaType;
    }

    @Override
    public Coalesce<T> value(T value) {
        return value(new LiteralExpression<T>(criteriaBuilder, value));
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public Coalesce<T> value(Expression<? extends T> value) {
        expressions.add(value);
        if (javaType == null) {
            javaType = (Class<T>) value.getJavaType();
        }
        return this;
    }

    public List<Expression<? extends T>> getExpressions() {
        return expressions;
    }

    @Override
    public void visitParameters(ParameterVisitor visitor) {
        for (Expression<?> expr : getExpressions()) {
            visitor.visit(expr);
        }
    }

    @Override
    public void render(RenderContext context) {
        final List<Expression<? extends T>> exprs = getExpressions();
        final StringBuilder buffer = context.getBuffer();
        buffer.append("COALESCE(");
        for (int i = 0; i < exprs.size(); i++) {
            if (i != 0) {
                buffer.append(',');
            }
            
            context.apply(exprs.get(i));
        }
        buffer.append(')');
    }
}