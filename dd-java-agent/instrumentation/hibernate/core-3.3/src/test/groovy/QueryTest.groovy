import datadog.trace.api.DDSpanTypes
import datadog.trace.api.DDTags
import datadog.trace.instrumentation.api.Tags
import org.hibernate.Query
import org.hibernate.Session

class QueryTest extends AbstractHibernateTest {

  def "test hibernate query.#queryMethodName single call"() {
    setup:

    // With Transaction
    Session session = sessionFactory.openSession()
    session.beginTransaction()
    queryInteraction(session)
    session.getTransaction().commit()
    session.close()

    // Without Transaction
    if (!requiresTransaction) {
      session = sessionFactory.openSession()
      queryInteraction(session)
      session.close()
    }

    expect:
    assertTraces(requiresTransaction ? 1 : 2) {
      // With Transaction
      trace(0, 4) {
        span(0) {
          operationName "hibernate.session"
          parent()
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            defaultTags()
          }
        }
        span(1) {
          operationName "hibernate.transaction.commit"
          childOf span(0)
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            defaultTags()
          }
        }
        span(2) {
          operationName "hibernate.$queryMethodName"
          childOf span(0)
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.RESOURCE_NAME" resource
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            "$Tags.DB_STATEMENT" queryMethodName == "iterate" ? null : String
            defaultTags()
          }
        }
        span(3) {
          childOf span(2)
          tags {
            "$DDTags.SERVICE_NAME" "h2"
            "$DDTags.RESOURCE_NAME" String
            "$DDTags.SPAN_TYPE" "sql"
            "$Tags.COMPONENT" "java-jdbc-prepared_statement"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            "$Tags.DB_TYPE" "h2"
            "$Tags.DB_INSTANCE" "db1"
            "$Tags.DB_USER" "sa"
            "$Tags.DB_STATEMENT" String
            "span.origin.type" "org.h2.jdbc.JdbcPreparedStatement"
            defaultTags()
          }
        }
      }
      if (!requiresTransaction) {
        // Without Transaction
        trace(1, 3) {
          span(0) {
            operationName "hibernate.session"
            parent()
            tags {
              "$DDTags.SERVICE_NAME" "hibernate"
              "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
              "$Tags.COMPONENT" "java-hibernate"
              "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
              defaultTags()
            }
          }
          span(1) {
            operationName "hibernate.$queryMethodName"
            childOf span(0)
            tags {
              "$DDTags.SERVICE_NAME" "hibernate"
              "$DDTags.RESOURCE_NAME" resource
              "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
              "$Tags.COMPONENT" "java-hibernate"
              "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
              "$Tags.DB_STATEMENT" queryMethodName == "iterate" ? null : String
              defaultTags()
            }
          }
          span(2) {
            childOf span(1)
            tags {
              "$DDTags.SERVICE_NAME" "h2"
              "$DDTags.RESOURCE_NAME" ~/^select /
              "$DDTags.SPAN_TYPE" "sql"
              "$Tags.COMPONENT" "java-jdbc-prepared_statement"
              "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
              "$Tags.DB_TYPE" "h2"
              "$Tags.DB_INSTANCE" "db1"
              "$Tags.DB_USER" "sa"
              "$Tags.DB_STATEMENT" ~/^select /
              "span.origin.type" "org.h2.jdbc.JdbcPreparedStatement"
              defaultTags()
            }
          }
        }
      }
    }

    where:
    queryMethodName       | resource     | requiresTransaction | queryInteraction
    "query.list"          | "Value"      | false               | { sess ->
      Query q = sess.createQuery("from Value")
      q.list()
    }
    "query.executeUpdate" | null         | true                | { sess ->
      Query q = sess.createQuery("update Value set name = 'alyx'")
      q.executeUpdate()
    }
    "query.uniqueResult"  | "Value"      | false               | { sess ->
      Query q = sess.createQuery("from Value where id = 1")
      q.uniqueResult()
    }
    "iterate"             | "from Value" | false               | { sess ->
      Query q = sess.createQuery("from Value")
      q.iterate()
    }
    "query.scroll"        | null         | false               | { sess ->
      Query q = sess.createQuery("from Value")
      q.scroll()
    }
  }

  def "test hibernate query.iterate"() {
    setup:

    Session session = sessionFactory.openSession()
    session.beginTransaction()
    Query q = session.createQuery("from Value")
    Iterator it = q.iterate()
    while (it.hasNext()) {
      it.next()
    }
    session.getTransaction().commit()
    session.close()

    expect:
    assertTraces(1) {
      trace(0, 4) {
        span(0) {
          operationName "hibernate.session"
          parent()
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            defaultTags()
          }
        }
        span(1) {
          operationName "hibernate.transaction.commit"
          childOf span(0)
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            defaultTags()
          }
        }
        span(2) {
          operationName "hibernate.iterate"
          childOf span(0)
          tags {
            "$DDTags.SERVICE_NAME" "hibernate"
            "$DDTags.RESOURCE_NAME" "from Value"
            "$DDTags.SPAN_TYPE" DDSpanTypes.HIBERNATE
            "$Tags.COMPONENT" "java-hibernate"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            defaultTags()
          }
        }
        span(3) {
          childOf span(2)
          tags {
            "$DDTags.SERVICE_NAME" "h2"
            "$DDTags.RESOURCE_NAME" ~/^select /
            "$DDTags.SPAN_TYPE" "sql"
            "$Tags.COMPONENT" "java-jdbc-prepared_statement"
            "$Tags.SPAN_KIND" Tags.SPAN_KIND_CLIENT
            "$Tags.DB_TYPE" "h2"
            "$Tags.DB_INSTANCE" "db1"
            "$Tags.DB_USER" "sa"
            "$Tags.DB_STATEMENT" ~/^select /
            "span.origin.type" "org.h2.jdbc.JdbcPreparedStatement"
            defaultTags()
          }
        }
      }
    }
  }

}
