import customer.Customer;
import customer.CustomerDao;
import customer.CustomerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Тестирование класса {@link CustomerService}
 *
 * @author Пыжьянов Вячеслав
 */
@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerDao customerDaoMock;
    /*
     Или так (если без аннотаций):
     private CustomerDao customerDaoMock = Mockito.mock(CustomerDao.class);
    */

    @InjectMocks
    private CustomerService customerService;
    /*
    Или так (если без аннотаций):
    private CustomerService customerService
            = new CustomerService(customerDaoMock);
    */

    /**
     * А ещё можно создать конструктор и в качестве параметров определить mock объекты
     */
/*    public CustomerServiceTest(@Mock CustomerDao customerDaoMock) {
        this.customerDaoMock = customerDaoMock;
        customerService = new CustomerService(customerDaoMock);
    }*/

    /**
     * Тестирование добавления покупателя
     */
    @Test
    public void testAddCustomer() throws Exception {
        Customer customer = new Customer(0, "11-11-11");

        Mockito.when(customerDaoMock.save(Mockito.eq(customer)))
                .thenReturn(Boolean.TRUE);

        Assertions.assertTrue(customerService.addCustomer(customer));

        Mockito.verify(customerDaoMock, Mockito.times(1))
                .exists(Mockito.eq("11-11-11"));
        Mockito.verify(customerDaoMock, Mockito.never())
                .delete(Mockito.any(Customer.class));
    }

    /**
     * Тестирование отсутствия сохранения при добавлении покупателя с таким же телефоном
     */
    @Test
    public void testNotSaveCustomerWithSamePhone() throws Exception {
        Mockito.when(customerDaoMock.exists(Mockito.any(String.class)))
                .thenReturn(Boolean.TRUE);

        Customer customer = new Customer(0, "11-11-11");
        Assertions.assertFalse(customerService.addCustomer(customer));
    }

    /**
     * Тестирование корректной обработки ошибки, возникшей в БД.
     * <p>Показательный пример: Кинуть исключение из mock объекта
     * и проверить, что оно обработано в нашем сервисе</p>
     */
    @Test
    public void testAddCustomerThrowsException() {
        Mockito.when(customerDaoMock.save(Mockito.any(Customer.class)))
                .thenThrow(RuntimeException.class);

        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            Customer customer = new Customer(0, "11-11-11");
            customerService.addCustomer(customer);
        });
        // Проверка сообщения об ошибке (что это человекочитаемая ошибка)
        Assertions.assertEquals("Не удалось добавить покупателя",
                exception.getMessage());
    }

    /**
     * Показательный пример: более детальная проверка аргумента вызываемой функции.
     * <p>Можем научиться отличать объекты, которые равны по equals</p>
     */
    @Test
    public void testArgThatExample() throws Exception {
        // Создаём покупателей, у которых одинаковые идентификаторы, т.е. они равны по equals
        Customer customer1 = new Customer(0, "11-11-11");
        Customer customer2 = new Customer(0, "22-22-22");

        customerService.addCustomer(customer1);

        // Задача: проверить что вызвался метод сохранения именно для customer1, но не для customer2
        // Такая проверка пройдёт (но этого нам не надо):
        Mockito.verify(customerDaoMock).save(Mockito.eq(customer2));

        Mockito.verify(customerDaoMock, Mockito.times(1))
                .save(Mockito.argThat(customer ->
                        customer.getPhone().equals("11-11-11")));
        Mockito.verify(customerDaoMock, Mockito.never())
                .save(Mockito.argThat(customer ->
                        customer.getPhone().equals("22-22-22")));
    }

    /**
     * Показательный пример: использование класса Answer, для установки id.
     * <p>Эмулируем поведение базы данных, а именно генерацию идентификатора</p>
     */
    @Test
    public void testAddCustomerWithId() throws Exception {

        // Using Answer to set an id to the customer which is passed in as a parameter to the mock method.
        Mockito.when(customerDaoMock.save(Mockito.any(Customer.class)))
                .thenAnswer((Answer<Boolean>) invocation -> {

                    Object[] arguments = invocation.getArguments();

                    if (arguments != null && arguments.length > 0 && arguments[0] != null) {

                        Customer customer = (Customer) arguments[0];
                        customer.setId(1);

                        return Boolean.TRUE;
                    }

                    return Boolean.FALSE;
                });

        Customer customer = new Customer(0, "11-11-11");

        Assertions.assertTrue(customerService.addCustomer(customer));
        Assertions.assertTrue(customer.getId() > 0);

    }
}