package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.TestClasses
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.dsl.SchemaBuilderDSL
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before


abstract class BaseSchemaTest {

    //test film 1
    val tomHardy = TestClasses.Actor("Tom Hardy", 232)
    val christianBale = TestClasses.Actor("Christian Bale", 232)
    val christopherNolan = TestClasses.Director("Christopher Nolan", 43, listOf(tomHardy, christianBale))
    val prestige = TestClasses.Film(TestClasses.Id("Prestige", 2006), 2006, "Prestige", christopherNolan)

    //test film 2
    val bradPitt = TestClasses.Actor("Brad Pitt", 763)
    val morganFreeman = TestClasses.Actor("Morgan Freeman", 1212)
    val kevinSpacey = TestClasses.Actor("Kevin Spacey", 2132)
    val davidFincher = TestClasses.Director("David Fincher", 43, listOf(bradPitt, morganFreeman, kevinSpacey))
    val se7en = TestClasses.Film(TestClasses.Id("Se7en", 1995), 1995, "Se7en", davidFincher)

    //new actors created via mutations in schema
    val createdActors = mutableListOf<TestClasses.Actor>()

    val testedSchema = SchemaBuilderDSL {
        query {
            name = "film"
            description = "mock film"
            resolver { -> prestige }
        }
        query {
            name = "filmByRank"
            description = "ranked films"
            resolver { rank: Int -> when(rank){
                1 -> prestige
                2 -> se7en
                else -> null
            }}
        }
        query {
            name = "filmsByType"
            description = "film categorized by type"
            resolver {type: TestClasses.FilmType -> listOf(prestige, se7en) }
        }
        query {
            name = "people"
            description = "List of all people"
            resolver { -> listOf(davidFincher, bradPitt, morganFreeman, christianBale, christopherNolan) }
        }
        query {
            name = "randomPerson"
            description = "not really random person"
            resolver { -> davidFincher as TestClasses.Person /*not really random*/}
        }
        mutation {
            name = "createActor"
            description = "create new actor"
            resolver { name : String, age : Int ->
                val actor = TestClasses.Actor(name, age)
                createdActors.add(actor)
                actor
            }
        }
        scalar(TestClasses.IdScalarSupport(), "unique, concise representation of film")
        enum<TestClasses.FilmType>("type of film, base on its length")

        type<TestClasses.Person>("Common data for any person")
    }.build() as DefaultSchema

    @Before
    fun cleanup() = createdActors.clear()

    fun assertNoErrors(map : Map<*,*>) {
        if(map["errors"] != null) throw AssertionError("Errors encountered: ${map["errors"]}")
        if(map["data"] == null) throw AssertionError("Data is null")
    }

    fun assertError(map : Map<*,*>, vararg messageElements : String) {
        val errorMessage = extract<String>(map, "errors/message")
        MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())

        messageElements
                .filterNot { errorMessage.contains(it) }
                .forEach { throw AssertionError("Expected error message to contain $it, but was: $errorMessage") }
    }

    fun execute(query: String, variables : String? = null) = deserialize(testedSchema.handleRequest(query, variables))
}