package com.zoowii.jpa_utils.query;

import com.google.common.base.Function;
import com.zoowii.jpa_utils.orm.Model;
import com.zoowii.jpa_utils.util.ListUtil;
import com.zoowii.jpa_utils.util.StringUtil;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query<M extends Model> {
    protected String tableName = null;  // TODO: select from multi-tables
    protected Class cls = null;
    protected List<OrderBy> orderBys = new ArrayList<>();
    protected Expr condition = Expr.dummy();
    protected String _tableSymbol = null;
    protected int _limit = -1;
    protected int _offset = -1;
    protected Map<Integer, Object> indexParameters = new HashMap<>();
    protected Map<String, Object> mapParameters = new HashMap<>();

    public String getTableSymbol() {
        if (_tableSymbol == null) {
            _tableSymbol = StringUtil.randomString(5);
        }
        return _tableSymbol;
    }

    /**
     * @param tableName maybe `User` or `User user`(then you can use user.name='abc' in expr)
     */
    public Query(String tableName) {
        this.tableName = tableName;
    }

    public Query(Class cls) {
        this.cls = cls;
        this.tableName = cls.getSimpleName();
    }

    public Query<M> limit(int limit) {
        _limit = limit;
        return this;
    }

    public Query<M> setMaxRows(int limit) {
        return limit(limit);
    }

    public Query<M> offset(int offset) {
        this._offset = offset;
        return this;
    }

    public Query<M> where(Expr expr) {
        this.condition = expr;
        return this;
    }

    public Query<M> clone() {
        Query<M> query = new Query<M>(tableName);
        query._limit = this._limit;
        query._offset = this._offset;
        query._tableSymbol = this._tableSymbol;
        query.condition = this.condition;
        query.orderBys = this.orderBys; // clone it
        query.indexParameters = this.indexParameters; // clone it
        query.mapParameters = this.mapParameters; // clone it
        return query;
    }

    public Query<M> setParameter(String key, Object value) {
        this.mapParameters.put(key, value);
        return this;
    }

    public Query<M> setParameter(int index, Object value) {
        this.indexParameters.put(index, value);
        return this;
    }

    public Query<M> eq(String name, Object value) {
        this.condition = this.condition.eq(name, value);
        return this;
    }

    public Query<M> ne(String name, Object value) {
        this.condition = this.condition.ne(name, value);
        return this;
    }

    public Query<M> gt(String name, Object value) {
        this.condition = this.condition.gt(name, value);
        return this;
    }

    public Query<M> ge(String name, Object value) {
        this.condition = this.condition.ge(name, value);
        return this;
    }

    public Query<M> like(String name, Object value) {
        this.condition = this.condition.like(name, value);
        return this;
    }

    public Query<M> lt(String name, Object value) {
        this.condition = this.condition.lt(name, value);
        return this;
    }

    public Query<M> le(String name, Object value) {
        this.condition = this.condition.le(name, value);
        return this;
    }

    public Query<M> or(Expr expr1, Expr expr2) {
        this.condition = this.condition.and(expr1.or(expr2));
        return this;
    }

    public Query<M> and(Expr expr) {
        this.condition = this.condition.and(expr);
        return this;
    }

    public Query<M> orderBy(String sort) {
        return orderBy(sort, true);
    }

    public Query<M> orderBy(String sort, boolean asc) {
        this.orderBys.add(new OrderBy(sort, asc));
        return this;
    }

    public Query<M> asc(String sort) {
        return orderBy(sort, true);
    }

    public Query<M> desc(String sort) {
        return orderBy(sort, false);
    }

    public String getOrderByString() {
        final Query _this = this;
        List<String> orderByStrs = ListUtil.map(this.orderBys, orderBy -> orderBy != null ? orderBy.toOrderByString(_this) : null);
        return StringUtil.join(orderByStrs, ",");
    }

    public Map<String, Object> toQuery() {
        String queryStr = "from " + tableName + " ";
        Map<String, Object> exprQuery = this.condition != null ? this.condition.toQueryString(this) : null;
        if (exprQuery != null) {
            queryStr += " where " + exprQuery.get("query");
        }
        if (this.orderBys.size() > 0) {
            queryStr += " order by " + this.getOrderByString();
        }
        ParameterBindings bindings = new ParameterBindings();
        if (exprQuery != null) {
            bindings = (ParameterBindings) exprQuery.get("bindings");
        }
        Map<String, Object> extras = ListUtil.hashmap("dummy", "dummy");
        if (this._limit >= 0) {
            extras.put("max", this._limit);
        }
        if (this._offset >= 0) {
            extras.put("offset", this._offset);
        }
        return ListUtil.hashmap("query", queryStr, "bindings", bindings, "extras", extras);
    }

    public long count() {
        if (this.cls == null) {
            throw new RuntimeException("you need pass a model class");
        }
        return count(this.cls);
    }

    public long count(Class model) {
        return (Long) getTypedQuery(Long.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return "select count(*) " + s;
            }
        }).getSingleResult();
    }

    public List<M> all() {
        if (this.cls == null) {
            throw new RuntimeException("you need pass a model class");
        }
        return all(this.cls);
    }

    public TypedQuery getTypedQuery(Class model) {
        return getTypedQuery(model, null);
    }

    /**
     * @param model        要查询的model
     * @param queryWrapper 用来对HQL进行二次处理,处理后再用来执行
     */
    public TypedQuery getTypedQuery(Class model, Function<String, String> queryWrapper) {
        Map<String, Object> query = this.toQuery();
        String queryString = (String) query.get("query");
        if (queryWrapper != null) {
            queryString = queryWrapper.apply(queryString);
        }
        TypedQuery typedQuery = M.getSession().getEntityManager().createQuery(queryString, model);
        ParameterBindings bindings = (ParameterBindings) query.get("bindings");
        if (bindings != null) {
            for (int i = 0; i < bindings.getIndexBindings().size(); ++i) {
                typedQuery.setParameter(i + 1, bindings.getIndexBindings().get(i));
            }
            for (String key : bindings.getMapBindings().keySet()) {
                typedQuery.setParameter(key, bindings.getMapBindings().get(key));
            }
        }
        Map<String, Object> extras = (Map<String, Object>) query.get("extras");
        if (extras != null) {
            if (extras.containsKey("max")) {
                typedQuery.setMaxResults((Integer) extras.get("max"));
            }
            if (extras.containsKey("offset")) {
                typedQuery.setFirstResult((Integer) extras.get("offset"));
            }
        }
        return typedQuery;
    }

    public List<M> findList() {
        return all();
    }

    public List<M> all(Class model) {
        return getTypedQuery(model).getResultList();
    }

    public M first() {
        if (this.cls == null) {
            throw new RuntimeException("you need pass a model class");
        }
        return first(this.cls);
    }

    public M findUnique() {
        return first();
    }

    public M first(Class model) {
        return (M) getTypedQuery(model).setMaxResults(1).getSingleResult();
    }
}
