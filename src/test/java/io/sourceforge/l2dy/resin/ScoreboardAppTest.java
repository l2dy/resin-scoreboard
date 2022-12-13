package io.sourceforge.l2dy.resin;

import com.sun.tools.attach.VirtualMachine;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ScoreboardAppTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ScoreboardAppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ScoreboardAppTest.class);
    }

    /**
     * Simple test
     */
    public void testApp() {
        assertNotNull(VirtualMachine.list());
    }
}
