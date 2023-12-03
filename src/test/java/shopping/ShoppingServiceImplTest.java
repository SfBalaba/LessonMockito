package shopping;

import customer.Customer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import product.Product;
import product.ProductDao;
import shopping.BuyException;
import shopping.Cart;
import shopping.ShoppingServiceImpl;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.argThat;


@ExtendWith(MockitoExtension.class)
public class ShoppingServiceImplTest {
    @Mock
    ProductDao productDao;
    @InjectMocks
    ShoppingServiceImpl shoppingService;
    private Product product;
    private Customer customer1;
    private Customer customer2;
    private Product product1;
    private List<Product> products;


    @Test
    void getCart() {
        // Почему создаётся новая корзина, хотя метод называется get?
        // Тогда метод должен называться createCart потому, что он возвращает новую корзину

        // Почему нельзя купить ровно столко товаров, сколько осталось на складе?
    }

    /**
     * Поведение определяем припомощи Mockito,
     * значит, что тестировать не нужно, вспомогательный метод
     */
    @Test
    void getAllProducts() {

    }

    /**
     * Здесь нечего тестировать,
     * используем в каечстве вспомогательного метода
     * для тестирования основной функциональности
     */
    @Test
    void getProductByName() {
    }

    @BeforeEach
    void setUp(){
        product = new Product("test product", 3);
        product1 = new Product("test product1", 5);
        customer1 = new Customer(1L, "11-11-11");
        customer2 = new Customer(2L, "11-11-11");
        products = List.of(product, product1);
    }

    /**
     * Добавление в корзину товара, если нет необходимого количества
     */
    @Test
    public void AddToCartIfCountLessThanInStock(){
        Cart cart = shoppingService.getCart(customer1);
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            cart.add(product, 4);
        });
        Assertions.assertEquals("Невозможно добавить товар " +
                        "'test product' в корзину, т.к. нет необходимого количества товаров",
                exception.getMessage());
    }

    /**
     * Успешное добавление товара в корзину
     * Валидация добавления в корзину прошла успешно
     */
    @Test
    public void getCartSuccess(){
        Cart cart = shoppingService.getCart(customer1);
        cart.add(product, 1);
        Assertions.assertEquals(1, cart.getProducts().size());
        Assertions.assertEquals(1, cart.getProducts().get(product));
        Assertions.assertEquals("test product",
                new ArrayList<>(cart.getProducts().keySet()).iterator().next().getName());
    }

    /**
     * Ситуация, когда валидация в момент добавления в корзину прошла успешно,
     * однако на момент покупки количество товара на складе было меьнше, чем в корзине
     * @throws BuyException ошибка покупки
     */
    @Test
    public void buyIfCountLessThanInStock() throws BuyException {
        Cart cart = shoppingService.getCart(customer1);
        Cart cart1 = shoppingService.getCart(customer2);

        cart.add(product, 2);
        cart1.add(product, 2);
        shoppingService.buy(cart1);

        Exception exception = Assertions.assertThrows(BuyException.class, () -> {
            shoppingService.buy(cart);
        });
        Assertions.assertEquals("В наличии нет необходимого количества товара 'test product'", exception.getMessage());
    }

    /**
     * Успешная покупка товара. Ситуация, когда товара в наличие имеется в нужном количестве на момент покупки
     * Продразумевается, что после покупки корзины, она должна очиститься
     */
    @Test
    public void buySuccess() throws BuyException {
        Mockito.when(shoppingService.getAllProducts()).thenReturn(products);
        Cart cart1 = shoppingService.getCart(customer1);
        cart1.add(product, 2);
        boolean result = shoppingService.buy(cart1);
        Assertions.assertTrue(result);
        Assertions.assertEquals(2, shoppingService.getAllProducts().size());
        // Проверяем количество первого продукта, который мы купили
        Assertions.assertEquals(1, shoppingService.getAllProducts().get(0).getCount());
        Mockito.verify(productDao, Mockito.atLeast(1))
                .save(Mockito.argThat((Product prod) -> prod.getName().equals("test product") && prod.getCount() == 1));
        Assertions.assertEquals(0, cart1.getProducts().size());
    }

    /**
     * Тест на изменение товаров в корзине
     */
    @Test
    public void buyEdit() throws BuyException {
        Cart cart1 = shoppingService.getCart(customer1);
        cart1.add(product1, 2);
        cart1.edit(product1, 1);
        boolean result = shoppingService.buy(cart1);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, product.getCount());
        //уменьшилось количество только того товара, который мы купили на 1 единицу, т.е. изменения учлись
        Assertions.assertEquals(4, product1.getCount());

        Assertions.assertEquals(1, cart1.getProducts().size());
        Assertions.assertEquals(1, cart1.getProducts().values().iterator().next());
        Assertions.assertEquals("test product1", cart1.getProducts().keySet().iterator().next().getName());
        Mockito.verify(productDao, Mockito.times(1))
                .save(argThat(prod -> prod.getName().equals("test product1") && prod.getCount() == 4));
    }

    /**
     * <p>Тест случая, когда на складе остался то количество товара, которое и хочет купить пользователь</p>
     * <p>Валдация не пройдёт, хотя хотелось бы урвать последний товар</p>
     * <p>Я считаю, что можно удалить запись в {@link ProductDao} о товаре, которого больше не осталось,
     * т.к. хранить товар в кодичестве 0 штук странно<p>
     */
    @Test
    void buyIfLastProducts() throws BuyException {
        Cart cart = shoppingService.getCart(customer1);
        cart.add(product, product.getCount());
        boolean result = shoppingService.buy(cart);

        Assertions.assertTrue(result);
        Assertions.assertEquals(0, product.getCount());
    }

    /**
     * Покупка пустой корзины
     */
    @Test
    void buyIfCartIsEmpty() throws BuyException {
        Cart cart = shoppingService.getCart(customer1);
        boolean result = shoppingService.buy(cart);
        Assertions.assertFalse(result);
    }

    /**
     * При попытке получить вторую корзину возвращается пустая корзина
     */
    @Test
    public void testGetCart(){
        Cart cart1 = shoppingService.getCart(customer1);
        cart1.add(product, 1);
        Cart cart2 = shoppingService.getCart(customer1);
        Assertions.assertNotEquals(cart1, cart2);
        Assertions.assertTrue(cart2.getProducts().isEmpty());
    }

    /**
     * Нет проверки на добавление в корзину отрицательного количества товара
     */
    @Test
    void buyIfProductCountIsNegativeOrZero() throws BuyException {
        Cart cart1 = shoppingService.getCart(customer1);
        cart1.add(product, -1);
        boolean result = shoppingService.buy(cart1);

        Assertions.assertFalse(result);
        Mockito.verify(productDao, Mockito.never())
                .save(Mockito.argThat(prod -> prod.getCount() == 4 && prod.getName().equals("test product")));
        Assertions.assertNotEquals(4, product1.getCount());
    }
}
