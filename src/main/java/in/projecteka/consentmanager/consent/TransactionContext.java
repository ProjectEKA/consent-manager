package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.Query;
import io.vertx.core.AsyncResult;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Transaction;
import reactor.core.publisher.MonoSink;

import java.util.Iterator;

public class TransactionContext {
    private Transaction transaction;
    private MonoSink<Void> sink;
    private final String txErrorMsg;

    public TransactionContext(Transaction transaction, MonoSink<Void> sink, String txErrorMsg) {
        this.transaction = transaction;
        this.sink = sink;
        this.txErrorMsg = txErrorMsg;
    }

    private void commit() {
        this.transaction.commit(result -> {
            if (result.succeeded()) {
                this.sink.success();
            } else {
                error(new RuntimeException(this.txErrorMsg, result.cause()));
            }
        });
    }

    private void error(RuntimeException e) {
        this.transaction.rollback();
        this.sink.error(e);
    }

    public void executeInTransaction(Iterator<Query> iterator) {
        if (iterator.hasNext()) {
            Query query = iterator.next();
            boolean hasParams = query.getParams() != null && query.getParams().size() > 0;
            if (!hasParams) {
                transaction.preparedQuery(query.getQueryString())
                        .execute(handler -> handleResponseAndExecNext(handler, iterator));
            } else {
                transaction.preparedQuery(query.getQueryString())
                        .execute(query.getParams(),
                                handler -> handleResponseAndExecNext(handler, iterator));
            }
        }
    }

    private void handleResponseAndExecNext(AsyncResult<RowSet<Row>> handler, Iterator<Query> iterator) {
        if (handler.succeeded()) {
            if (iterator.hasNext()) {
                executeInTransaction(iterator);
            } else {
                commit();
            }
        } else {
            error(new RuntimeException(this.txErrorMsg, handler.cause()));
        }
    }
}
