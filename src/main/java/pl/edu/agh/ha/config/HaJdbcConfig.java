package pl.edu.agh.ha.config;

import net.sf.hajdbc.SimpleDatabaseClusterConfigurationFactory;
import net.sf.hajdbc.SynchronizationStrategy;
import net.sf.hajdbc.balancer.BalancerFactory;
import net.sf.hajdbc.balancer.random.RandomBalancerFactory;
import net.sf.hajdbc.balancer.roundrobin.RoundRobinBalancerFactory;
import net.sf.hajdbc.balancer.simple.SimpleBalancerFactory;
import net.sf.hajdbc.cache.DatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.eager.EagerDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.eager.SharedEagerDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.lazy.LazyDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.lazy.SharedLazyDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.cache.simple.SimpleDatabaseMetaDataCacheFactory;
import net.sf.hajdbc.sql.Driver;
import net.sf.hajdbc.sql.DriverDatabase;
import net.sf.hajdbc.sql.DriverDatabaseClusterConfiguration;
import net.sf.hajdbc.state.StateManagerFactory;
import net.sf.hajdbc.state.bdb.BerkeleyDBStateManagerFactory;
import net.sf.hajdbc.state.simple.SimpleStateManagerFactory;
import net.sf.hajdbc.state.sql.SQLStateManagerFactory;
import net.sf.hajdbc.state.sqlite.SQLiteStateManagerFactory;
import net.sf.hajdbc.sync.*;
import net.sf.hajdbc.util.concurrent.cron.CronExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@ConfigurationProperties(prefix = "hajdbc")
@ConditionalOnClass(Driver.class)
@Configuration
class HaJdbcConfig {
    private List<DriverDatabase> driverDatabases = new ArrayList<>();
    private String clusterName = "default";
    private String cronExpression = "0 0/1 * 1/1 * ? *";
    private String balancerFactory = "round-robin";
    private String defaultSynchronizationStrategy = "full";
    private String databaseMetaDataCacheFactory = "shared-eager";
    private String stateManagerFactory = "simple";
    private String stateManagerUrl;
    private String stateManagerUser;
    private String stateManagerPassword;
    private String stateManagerLocation;
    private boolean identityColumnDetectionEnabled = true;
    private boolean sequenceDetectionEnabled = true;

    public void register() throws ParseException {
        DriverDatabaseClusterConfiguration config = new DriverDatabaseClusterConfiguration();
        config.setDatabases(driverDatabases);
        config.setDatabaseMetaDataCacheFactory(DatabaseMetaDataCacheChoice.fromId(databaseMetaDataCacheFactory));
        config.setBalancerFactory(BalancerChoice.fromId(balancerFactory));
        config.setStateManagerFactory(StateManagerChoice.fromId(stateManagerFactory));
        switch (stateManagerFactory) {
            case "sql":
                SQLStateManagerFactory mgr = (SQLStateManagerFactory) config.getStateManagerFactory();
                if (stateManagerUrl != null)
                    mgr.setUrlPattern(stateManagerUrl);
                if (stateManagerUser != null)
                    mgr.setUser(stateManagerUser);
                if (stateManagerPassword != null)
                    mgr.setPassword(stateManagerPassword);
                break;
            case "berkeleydb":
            case "sqlite":
                BerkeleyDBStateManagerFactory mgr2 = (BerkeleyDBStateManagerFactory) config.getStateManagerFactory();
                if (stateManagerLocation != null)
                    mgr2.setLocationPattern(stateManagerLocation);
                break;
        }
        config.setSynchronizationStrategyMap(SynchronizationStrategyChoice.getIdMap());
        config.setDefaultSynchronizationStrategy(defaultSynchronizationStrategy);
        config.setIdentityColumnDetectionEnabled(identityColumnDetectionEnabled);
        config.setSequenceDetectionEnabled(sequenceDetectionEnabled);
        config.setAutoActivationExpression(new CronExpression(cronExpression));
        Driver.setConfigurationFactory(clusterName, new SimpleDatabaseClusterConfigurationFactory<>(config));
    }

    enum DatabaseMetaDataCacheChoice {
        SIMPLE(new SimpleDatabaseMetaDataCacheFactory()),
        LAZY(new LazyDatabaseMetaDataCacheFactory()),
        EAGER(new EagerDatabaseMetaDataCacheFactory()),
        SHARED_LAZY(new SharedLazyDatabaseMetaDataCacheFactory()),
        SHARED_EAGER(new SharedEagerDatabaseMetaDataCacheFactory());
        DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory;

        DatabaseMetaDataCacheChoice(DatabaseMetaDataCacheFactory databaseMetaDataCacheFactory) {
            this.databaseMetaDataCacheFactory = databaseMetaDataCacheFactory;
        }

        static DatabaseMetaDataCacheFactory fromId(String id) {

            return Stream.of(values())
                    .filter(value -> value.databaseMetaDataCacheFactory.getId().equals(id))
                    .map(value -> value.databaseMetaDataCacheFactory)
                    .findFirst().orElse(null);
        }
    }

    enum BalancerChoice {
        ROUND_ROBIN(new RoundRobinBalancerFactory()),
        RANDOM(new RandomBalancerFactory()),
        SIMPLE(new SimpleBalancerFactory());
        BalancerFactory balancerFactory;

        BalancerChoice(BalancerFactory balancerFactory) {
            this.balancerFactory = balancerFactory;
        }

        static BalancerFactory fromId(String id) {
            return Stream.of(values()).filter(value -> value.balancerFactory.getId().equals(id))
                    .map(value -> value.balancerFactory)
                    .findFirst().orElse(null);
        }
    }

    enum SynchronizationStrategyChoice {
        FULL(new FullSynchronizationStrategy()),
        DUMP_RESTORE(new DumpRestoreSynchronizationStrategy()),
        DIFF(new DifferentialSynchronizationStrategy()),
        FASTDIFF(new FastDifferentialSynchronizationStrategy()),
        PER_TABLE_FULL(new PerTableSynchronizationStrategy(new FullSynchronizationStrategy())),
        PER_TABLE_DIFF(new PerTableSynchronizationStrategy(new DifferentialSynchronizationStrategy())),
        PASSIVE(new PassiveSynchronizationStrategy());
        SynchronizationStrategy synchronizationStrategy;

        SynchronizationStrategyChoice(SynchronizationStrategy synchronizationStrategy) {
            this.synchronizationStrategy = synchronizationStrategy;
        }

        static SynchronizationStrategy fromId(String id) {
            return Stream.of(values()).filter(value -> value.synchronizationStrategy.getId().equals(id))
                    .map(value -> value.synchronizationStrategy)
                    .findFirst().orElse(null);
        }

        static Map<String, SynchronizationStrategy> getIdMap() {
            return Stream.of(values()).collect(Collectors.toMap(value -> value.synchronizationStrategy.getId(), value -> value.synchronizationStrategy));
        }
    }

    enum StateManagerChoice {
        SIMPLE(new SimpleStateManagerFactory()),
        BERKELEYDB(new BerkeleyDBStateManagerFactory()),
        SQLITE(new SQLiteStateManagerFactory()),
        SQL(new SQLStateManagerFactory());
        private StateManagerFactory stateManagerFactory;

        StateManagerChoice(StateManagerFactory stateManagerFactory) {
            this.stateManagerFactory = stateManagerFactory;
        }

        static StateManagerFactory fromId(String id) {
            return Stream.of(values()).filter(value -> value.stateManagerFactory.getId().equals(id))
                    .map(value -> value.stateManagerFactory)
                    .findFirst().orElse(null);
        }
    }

    public List<DriverDatabase> getDriverDatabases() {
        return driverDatabases;
    }

    public void setDriverDatabases(List<DriverDatabase> driverDatabases) {
        this.driverDatabases = driverDatabases;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getBalancerFactory() {
        return balancerFactory;
    }

    public void setBalancerFactory(String balancerFactory) {
        this.balancerFactory = balancerFactory;
    }

    public String getDefaultSynchronizationStrategy() {
        return defaultSynchronizationStrategy;
    }

    public void setDefaultSynchronizationStrategy(String defaultSynchronizationStrategy) {
        this.defaultSynchronizationStrategy = defaultSynchronizationStrategy;
    }

    public String getDatabaseMetaDataCacheFactory() {
        return databaseMetaDataCacheFactory;
    }

    public void setDatabaseMetaDataCacheFactory(String databaseMetaDataCacheFactory) {
        this.databaseMetaDataCacheFactory = databaseMetaDataCacheFactory;
    }

    public String getStateManagerFactory() {
        return stateManagerFactory;
    }

    public void setStateManagerFactory(String stateManagerFactory) {
        this.stateManagerFactory = stateManagerFactory;
    }

    public String getStateManagerUrl() {
        return stateManagerUrl;
    }

    public void setStateManagerUrl(String stateManagerUrl) {
        this.stateManagerUrl = stateManagerUrl;
    }

    public String getStateManagerUser() {
        return stateManagerUser;
    }

    public void setStateManagerUser(String stateManagerUser) {
        this.stateManagerUser = stateManagerUser;
    }

    public String getStateManagerPassword() {
        return stateManagerPassword;
    }

    public void setStateManagerPassword(String stateManagerPassword) {
        this.stateManagerPassword = stateManagerPassword;
    }

    public String getStateManagerLocation() {
        return stateManagerLocation;
    }

    public void setStateManagerLocation(String stateManagerLocation) {
        this.stateManagerLocation = stateManagerLocation;
    }

    public boolean isIdentityColumnDetectionEnabled() {
        return identityColumnDetectionEnabled;
    }

    public void setIdentityColumnDetectionEnabled(boolean identityColumnDetectionEnabled) {
        this.identityColumnDetectionEnabled = identityColumnDetectionEnabled;
    }

    public boolean isSequenceDetectionEnabled() {
        return sequenceDetectionEnabled;
    }

    public void setSequenceDetectionEnabled(boolean sequenceDetectionEnabled) {
        this.sequenceDetectionEnabled = sequenceDetectionEnabled;
    }
}