package net.nutrolshok.diamondworldtesttask.mysql;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.nutrolshok.diamondworldtesttask.Main;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class MysqlThread extends Thread {

    static final int TICK_INTERVAL = 1000;
    static final String UNICODE_PARAMS = "useUnicode=true&characterEncoding=utf-8&useSSL=false";
    final MysqlConfig config;
    final Object lock;
    final ConcurrentLinkedQueue<Query> queries;
    final Logger logger;

    volatile boolean running, connected;

    Connection db;
    boolean useUnicode;

    public MysqlThread(@NotNull final Main plugin, @NotNull final MysqlConfig config) {
        this.useUnicode = false;
        this.lock = new Object();
        this.running = false;
        this.connected = false;
        this.setName(plugin.getName() + " - Mysql");
        this.config = config;
        this.logger = plugin.getLogger();
        this.queries = new ConcurrentLinkedQueue<>();
    }

    public void query(@NotNull final String query) {
        this.update(query, null);
    }

    public void update(@NotNull final String query, final UpdateCallback callback) {
        this.queries.add(new Query(query, callback));
        synchronized(this.lock) {
            this.lock.notify();
        }
    }

    public void start() {
        if(this.running) return;

        this.running = true;
        super.start();
    }

    public void finish() {
        if(!this.running) return;

        this.running = false;
        this.safe(this::join);

        if(this.db == null) return;

        this.safe(this::checkConnection);
        this.safe(this::executeQueries);

        this.safe(this.db::close);
    }

    public void useUnicode() {
        this.useUnicode = true;
    }

    protected void onConnect() {}

    protected void onDisconnect() {}

    protected String onPreQuery(@NotNull final String query) {
        return query;
    }

    protected void onPostQuery() {}

    protected void safe(@NotNull final SafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {}
    }

    public void run() {
        this.checkConnection();

        while(this.running) {
            if (!this.queries.isEmpty()) {
                if (this.checkConnection()) this.executeQueries();
                else this.queries.clear();
            }

            try {
                synchronized (this.lock) {
                    this.lock.wait(TICK_INTERVAL);
                }
            } catch (InterruptedException exception) {
                this.running = false;
            }
        }
    }

    private void executeQueries() {
        while(!this.queries.isEmpty()) {
            final Query query = this.queries.poll();
            final String q = this.onPreQuery(query.query);

            if (q == null) continue;

            try {
                final Statement statement = this.db.createStatement();
                Throwable throwable = null;
                try {
                    try {
                        if (statement.execute(q)) {
                            if (query.callback != null) {
                                final ResultSet rs = statement.getResultSet();
                                ((SelectCallback) query.callback).done(rs);
                                rs.close();
                            }
                        } else if (query.callback != null) ((UpdateCallback) query.callback).done(statement.getUpdateCount());
                    } catch (final Exception exception) {
                        this.logger.log(Level.SEVERE, "Query " + q + " is failed!", exception);
                    }
                    this.onPostQuery();
                } catch (final Throwable exception) {
                    throwable = exception;
                    throw exception;
                } finally {
                    if (statement != null) {
                        if (throwable != null) {
                            try {
                                statement.close();
                            } catch (Throwable throwable1) {
                                throwable.addSuppressed(throwable1);
                            }
                        } else statement.close();
                    }
                }
            } catch (final Exception exception) {
                this.onPostQuery();
                if (exception.getMessage() != null && exception.getMessage().contains("try restarting transaction")) {
                    this.queries.add(query);
                    this.logger.warning(" Query " + q + " is failed! Restarting: " + exception.getMessage());
                } else this.logger.severe("Query " + q + " is failed! Message: " + exception.getMessage());
            }
        }
    }

    private boolean checkConnection() {
        boolean state = false;

        try {
            if (this.db != null && !this.isValid()) {
                this.safe(this.db::close);
                this.db = null;
            }

            if (this.db == null) this.connect();

            state = this.db != null && this.isValid();
        } catch (Exception exception) {
            this.logger.log(Level.WARNING, "Error while connecting to database: {0}", exception.getMessage());
        }

        if (this.connected != state) {
            this.connected = state;
            if (!this.connected) this.onDisconnect();
        }

        return state;
    }

    private void connect() {
        try {
            this.db = DriverManager.getConnection(this.config.getUrl() + (this.useUnicode ? this.addUnicodeParams(this.config.getUrl()) : ""),
                    this.config.getUser(), this.config.getPass());

            if (this.isValid()) {
                this.logger.info("MySQL connected.");
                this.onConnect();
            }
        } catch (SQLException exception) {
            this.logger.warning(exception.getMessage());
        }
    }

    private String addUnicodeParams(@NotNull String url) {
        if (url.contains("?")) {
            if (url.contains(UNICODE_PARAMS)) return url;
            url += "&";
        } else url += "?";

        return url + UNICODE_PARAMS;
    }

    private boolean isValid() throws SQLException {
        return this.db.isValid(40);
    }

    @FieldDefaults(level = AccessLevel.PUBLIC)
    @AllArgsConstructor
    private static class Query {

        String query;
        Callback callback;

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class MysqlConfigSupplier implements MysqlConfig {

        Supplier<String> url, user, pass;

        public MysqlConfigSupplier(Supplier<String> url, Supplier<String> user, Supplier<String> pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }

        @Override
        public String getUrl() {
            return url.get();
        }

        @Override
        public String getUser() {
            return user.get();
        }

        @Override
        public String getPass() {
            return pass.get();
        }

    }

    protected interface SafeRunnable {

        void run() throws Exception;

    }

    public interface MysqlConfig {

        String getUrl();

        String getUser();

        String getPass();

    }

}
