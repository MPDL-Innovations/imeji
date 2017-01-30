package de.mpg.imeji.logic.vo.factory;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;

/**
 * Factory for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementFactory {

  private final Statement statement = new Statement();

  /**
   * Build the statement
   *
   * @return
   */
  public Statement build() {
    return statement;
  }

  public StatementFactory setIndex(String index) {
    statement.setIndex(index);
    return this;
  }

  public StatementFactory setType(StatementType type) {
    statement.setType(type);
    return this;
  }
}
