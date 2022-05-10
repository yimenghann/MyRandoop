import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ErrorTest1 {

    @Test
    public void test0() throws Throwable {
        org.joda.time.DateTimeField dateTimeField0 = null;
        org.joda.time.DateTimeField dateTimeField1 = org.joda.time.field.StrictDateTimeField.getInstance(dateTimeField0);
        dateTimeField1.toString();
        dateTimeField1.hashCode();
        org.junit.Assert.assertTrue(dateTimeField1.equals(dateTimeField1));
    }
}
