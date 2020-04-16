package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.Query;
import io.vertx.sqlclient.Transaction;
import reactor.core.publisher.MonoSink;

import java.util.Iterator;

public class TransactionContext {
    private static final String FAILED_TO_SAVE_CONSENT_ARTEFACT = "Failed to save consent artefact";
    private Transaction transaction;
    private MonoSink sink;

    public TransactionContext(Transaction transaction, MonoSink sink) {
        this.transaction = transaction;
        this.sink = sink;
    }

    private void commit() {
        this.transaction.commit(result -> {
            if (result.succeeded()) {
                this.sink.success();
            } else {
                error(new RuntimeException(result.cause().getMessage()));
            }
        });
    }

    private void error(RuntimeException e) {
        this.sink.error(e);
    }

    public void executeInTransaction(Iterator<Query> iterator) {
        if (iterator.hasNext()) {
            Query query = iterator.next();
            transaction.preparedQuery(query.getQueryString())
                    .execute(query.getParams(),
                            handler -> {
                                if (handler.succeeded()) {
                                    if (iterator.hasNext()) {
                                        executeInTransaction(iterator);
                                    } else {
                                        commit();
                                    }
                                } else {
                                    error(new RuntimeException(FAILED_TO_SAVE_CONSENT_ARTEFACT));
                                }
                            });
        }
    }
}
