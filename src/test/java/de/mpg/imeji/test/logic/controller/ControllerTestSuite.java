package de.mpg.imeji.test.logic.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.businesscontroller.InvitationBusinessControllerTest;
import de.mpg.imeji.testimpl.logic.businesscontroller.RegistratinBusinessControllerTest;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;
import de.mpg.imeji.testimpl.logic.controller.ShareControllerTestClass;
import de.mpg.imeji.testimpl.logic.controller.SpaceControllerTestClass;
import de.mpg.imeji.testimpl.logic.controller.StatisticsControllerTestClass;
import de.mpg.imeji.testimpl.logic.controller.UserControllerTestClass;
import util.SuperTestSuite;


@RunWith(Suite.class)
@Suite.SuiteClasses({ItemControllerTestClass.class, ShareControllerTestClass.class,
    SpaceControllerTestClass.class, StatisticsControllerTestClass.class,
    UserControllerTestClass.class, InvitationBusinessControllerTest.class,
    RegistratinBusinessControllerTest.class})
public class ControllerTestSuite extends SuperTestSuite {

}
