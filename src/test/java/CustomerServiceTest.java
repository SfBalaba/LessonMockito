import customer.Customer;
import customer.CustomerDao;
import customer.CustomerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Тестирование класса {@link CustomerService}
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
                .delete(ArgumentMatchers.any(Customer.class));
    }

    /**
     * Тестирование отсутствия сохранения при добавлении покупателя с таким же телефоном
     */
    @Test
    public void testNotSaveCustomerWithSamePhone() throws Exception {
        Mockito.when(customerDaoMock.exists(ArgumentMatchers.any(String.class)))
                .thenReturn(Boolean.TRUE);

        Customer customer = new Customer(0, "11-11-11");
        Assertions.assertFalse(customerService.addCustomer(customer));
    }

    /**
     * Показательный пример: использование класса Answer, для установки id
     */
    @Test
    public void testAddCustomerWithId() throws Exception {

        // Using Answer to set an id to the customer which is passed in as a parameter to the mock method.
        Mockito.when(customerDaoMock.save(ArgumentMatchers.any(Customer.class)))
                .thenAnswer((Answer<Boolean>) invocation -> {

            Object[] arguments = invocation.getArguments();

            if (arguments != null && arguments.length > 0 && arguments[0] != null){

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

    /**
     * Показательный пример: Кинуть исключение из mock объекта
     * и проверить, что оно обработано в нашем сервисе
     */
    @Test
    public void testAddCustomerThrowsException() {
        Mockito.when(customerDaoMock.save(ArgumentMatchers.any(Customer.class)))
                .thenThrow(RuntimeException.class);

        Exception exception = Assertions.assertThrows(Exception.class, () -> {
            Customer customer = new Customer(0, "11-11-11");
            customerService.addCustomer(customer);
        });
        // Проверка сообщения об ошибке (что это человекочитаемая ошибка)
        Assertions.assertEquals("Не удалось добавить покупателя", exception.getMessage());
    }
}