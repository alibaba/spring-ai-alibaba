package ai.spring.demo.ai.playground.client;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;

import java.util.List;

import ai.spring.demo.ai.playground.services.BookingTools.BookingDetails;
import ai.spring.demo.ai.playground.services.FlightBookingService;

@BrowserCallable
@AnonymousAllowed
public class BookingService {
    private final FlightBookingService flightBookingService;

    public BookingService(FlightBookingService flightBookingService) {
        this.flightBookingService = flightBookingService;
    }

    public List<BookingDetails> getBookings() {
        return flightBookingService.getBookings();
    }
}
