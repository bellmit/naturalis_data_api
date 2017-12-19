package nl.naturalis.nba.utils.reflect;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import org.junit.Test;

@SuppressWarnings("static-method")
public class ReflectionUtilTest {

  @Test
  public void testInstantiate() {
    Person p = ReflectionUtil.newInstance(Person.class, "John Smith");
    assertEquals("01", "John Smith", p.getName());
  }

  @Test
  public void testSet() {
    Person p = new Person();
    ReflectionUtil.set(p, "name", "John Smith");
    assertEquals("01", "John Smith", p.getName());
  }

  @Test
  public void testGet() {
    Person p = new Person();
    p.setName("John Smith");
    String s = ReflectionUtil.get(p, "name", String.class);
    assertEquals("01", "John Smith", s);
  }

  @Test
  public void testGetField_01() {
    Field f = ReflectionUtil.getField("name", Person.class);
    assertNotNull("01", f);
  }

  @Test
  public void testGetField_02() {
    Field f = ReflectionUtil.getField("foo", Person.class);
    assertNull("01", f);
  }

  @Test
  public void testCall_01() {
    Person p = new Person();
    p.setAge(15);;
    double d = ReflectionUtil.call(p, "calculateMinimumWage", double.class, 1000, true);
    // age * multiplier + bonus = 15 * 1000 + 200 = 15200
    assertTrue("01", 15200D == d);
  }

}
