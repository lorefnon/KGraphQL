package com.apurebase.kgraphql.specification.language

import com.apurebase.kgraphql.Actor
import com.apurebase.kgraphql.Specification
import com.apurebase.kgraphql.defaultSchema
import com.apurebase.kgraphql.deserialize
import com.apurebase.kgraphql.executeEqualQueries
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.6 Arguments")
class ArgumentsSpecificationTest {
    val age = 432

    val schema = defaultSchema {
        query("actor") {
            resolver { -> Actor("Boguś Linda", age) }
        }

        type<Actor>{
            property<List<String>>("favDishes") {
                resolver { _: Actor, size: Int, prefix: String? ->
                    listOf("steak", "burger", "soup", "salad", "bread", "bird").let { dishes ->
                        if(prefix != null){
                            dishes.filter { it.startsWith(prefix) }
                        } else {
                            dishes
                        }
                    }.take(size)
                }
            }
        }
    }

    @Test
    fun `arguments are unordered`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))),
                "{actor{favDishes(size: 2, prefix: \"b\")}}",
                "{actor{favDishes(prefix: \"b\", size: 2)}}"
        )
    }

    @Test
    fun `many arguments can exist on given field`(){
        val response = deserialize(schema.execute("{actor{favDishes(size: 2, prefix: \"b\")}}")) as Map<String, Any>
        assertThat (
                response, equalTo(mapOf<String, Any>("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))))
        )
    }


}