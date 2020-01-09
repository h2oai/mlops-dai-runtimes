package ai.h2o.mojos.deploy.common.jdbc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;

public class JdbcConfig {
  public final String dbConnectionString;
  public final String dbUser;
  public final String dbPassword;
  public final String dbDriver;

  /**
   * Small Helper class to contain configuration parameters of JDBC Scorer.
   *
   * @param path String set by system property that denotates where the config file exists.
   */
  public JdbcConfig(String path) {
    Config config = ConfigFactory.parseFile(new File(path));
    this.dbConnectionString = config.getString("db.connection");
    this.dbUser = config.getString("db.user");
    this.dbPassword = config.getString("db.password");
    this.dbDriver = config.getString("db.driver");
  }
}
