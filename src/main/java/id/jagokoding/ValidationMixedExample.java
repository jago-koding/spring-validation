package id.jagokoding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ValidationMixedExample {


    public static void main (String[] args) {
    	Locale.setDefault(new Locale("in", "ID"));
        AnnotationConfigApplicationContext context = new
                            AnnotationConfigApplicationContext(
                            Config.class);

        ClientBean clientBean = context.getBean(ClientBean.class);
        clientBean.processOrder();
    }


    @Configuration
    public static class Config {

        @Bean
        public ClientBean clientBean () {
            return new ClientBean();
        }

        @Bean
        public Order order () {
            Order order = new Order();
            //  order.setPrice(BigDecimal.TEN);
            //  order.setDate(new Date(System.currentTimeMillis() + 100000));
            order.setCustomerId("111");
            return order;
        }

        @Bean
        public org.springframework.validation.Validator validatorFactory () {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        public OrderValidator orderValidator () {
            return new OrderValidator();
        }

        @Bean
        public GenericValidator genericValidator () {
            return new GenericValidator();
        }
    }


    private static class ClientBean {
        @Autowired
        private Order order;

        @Autowired
        private GenericValidator genericValidator;

        private void processOrder () {
            if (genericValidator.validateObject(order)) {
                System.out.println("processing " + order);
            }
        }

        public Order getOrder () {
            return order;
        }
    }


    private static class Order {
        @NotNull(message = "{date.empty}")
        @Future(message = "{date.future}")
        private Date date;

        @NotNull(message = "{price.empty}")
        @DecimalMin(value = "0", inclusive = false, message = "{price.invalid}")
        private BigDecimal price;

        String customerId;

        public void setDate (Date date) {
            this.date = date;
        }

        public void setPrice (BigDecimal price) {
            this.price = price;
        }

        public Date getDate () {
            return date;
        }

        public BigDecimal getPrice () {
            return price;
        }

        public String getCustomerId () {
            return customerId;
        }

        public void setCustomerId (String customerId) {
            this.customerId = customerId;
        }

        @Override
        public String toString () {
            return "Order{ date=" + date + ", price=" + price +
                                ", customerId='" + customerId + "'}";
        }
    }

    private static class OrderValidator implements
                        org.springframework.validation.Validator {

        @Override
        public boolean supports (Class<?> clazz) {
            return Order.class == clazz;
        }

        @Override
        public void validate (Object target, Errors errors) {
            ValidationUtils.rejectIfEmpty(errors,
                                "customerId", "customerId.empty");

            Order order = (Order) target;
            if (order.getCustomerId() != null) {
                Customer customer = getCustomerById(order.getCustomerId());
                if (customer == null) {
                    errors.reject("customer.id.invalid",
                                        new Object[]{order.getCustomerId()},
                                        "Customer id is not valid");
                }
            }
        }

        private Customer getCustomerById (String customerId) {
        	// hanya untuk tes mengembalikan null ..
            // jika tidak kita harus menggunakan layanan data backend di sini
            return null;
        }
    }

    private static class Customer {
    }

    private static class GenericValidator {

        @Autowired
        ApplicationContext context;

        /**
         * Ini adalah metode umum gaya kerangka kerja yang dapat
         * memvalidasi objek apa pun yang sesuai
         * validator terdaftar sebagai bean.
         */
        public boolean validateObject (Object objectToValidate) {

            Map<String, Validator> validatorMap =
                                context.getBeansOfType(Validator.class);
            if (validatorMap == null) {
                return true;
            }

            DataBinder binder = new DataBinder(objectToValidate);

            // dalam contoh ini dua validator adalah register OrderValidator
            // dan LocalValidatorFactoryBean yang akan melakukan validasi JSR 349.
            for (Validator validator : validatorMap.values()) {

                if (validator.supports(objectToValidate.getClass())) {

                    binder.addValidators(validator);
                }
            }
            binder.validate();
            BindingResult bindingResult = binder.getBindingResult();
            if (bindingResult.hasErrors()) {
                ResourceBundleMessageSource messageSource =
                                    new ResourceBundleMessageSource();
                messageSource.setBasename("ValidationMessages");


                System.out.println(messageSource.getMessage("object.invalid",
                                    new Object[]{objectToValidate.getClass().
                                                        getSimpleName()},
                                    new Locale("in", "ID")));

                bindingResult.getAllErrors()
                             .stream()
                             .map(e -> messageSource.getMessage(e,
                            		 new Locale("in", "ID")))
                             .forEach(System.out::println);
                return false;
            }

            return true;
        }

    }
}