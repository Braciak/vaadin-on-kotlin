@file:Suppress("UNCHECKED_CAST")

package com.github.vok.framework.sql2o.vaadin

import com.github.vok.framework.flow.DefaultFilterFieldFactory
import com.github.vok.framework.flow.FilterFactory
import com.github.vok.framework.flow.FilterFieldFactory
import com.github.vok.framework.flow.appendHeaderAbove
import com.github.vokorm.*
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.grid.ColumnGroup
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.binder.BeanPropertySet
import com.vaadin.flow.data.binder.PropertyDefinition
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import kotlin.reflect.KClass
import kotlin.streams.*

/**
 * Produces filters defined by the `VoK-ORM` library. This will allow us to piggyback on the ability of `VoK-ORM` filters to produce
 * SQL92 WHERE clause. See [SqlDataProvider] and [EntityDataProvider] for more details.
 */
class SqlFilterFactory<T: Any> : FilterFactory<Filter<T>> {
    override fun and(filters: Set<Filter<T>>) = filters.and()
    override fun or(filters: Set<Filter<T>>) = filters.or()
    override fun eq(propertyName: String, value: Any) = EqFilter<T>(propertyName, value)
    override fun le(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.le)
    override fun ge(propertyName: String, value: Any) = OpFilter<T>(propertyName, value as Comparable<Any>, CompareOperator.ge)
    override fun ilike(propertyName: String, value: String) = ILikeFilter<T>(propertyName, value)
}

/**
 * Re-creates filters in this header row. Simply call `grid.appendHeaderRow().generateFilterComponents(grid)` to automatically attach
 * filters to non-generated columns. Please note that filters are not re-generated when the container data source is changed.
 * @param grid the owner grid.
 * @param filterFieldFactory used to create the filters themselves. If null, [DefaultFilterFieldFactory] is used.
 * @return map mapping property ID to the filtering component generated
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Grid<T>.generateFilterComponents(itemClass: KClass<T>,
                                                filterFieldFactory: FilterFieldFactory<T, Filter<T>> = DefaultFilterFieldFactory(itemClass.java,
                                                    { dataProvider as ConfigurableFilterDataProvider<T, Filter<T>?, Filter<T>?> },
                                                        SqlFilterFactory<T>())): Map<String, Component> {
    val properties: Map<String, PropertyDefinition<T, *>> = BeanPropertySet.get(itemClass.java).properties.toList().associateBy { it.name }
    val result = mutableMapOf<String, Component>()
    for (propertyId in columns.mapNotNull { it.key }) {
        val property = properties[propertyId]
        val field: HasValue<*, *>? = if (property == null) null else filterFieldFactory.createField(property)
        if (field != null) {
            filterFieldFactory.bind(field as HasValue<*, Any?>, property!! as PropertyDefinition<T, Any?>)
            getColumnByKey(propertyId).appendHeaderAbove(field as Component)
            result[propertyId] = field as Component
        }
    }
    return result
}
