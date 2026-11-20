package com.rentease.service;

import com.rentease.dto.booking.BookingDto;
import com.rentease.dto.booking.CreateBookingRequest;
import com.rentease.entity.Booking;
import com.rentease.entity.Listing;
import com.rentease.entity.User;
import com.rentease.enums.BookingStatus;
import com.rentease.enums.ListingCategory;
import com.rentease.enums.ListingStatus;
import com.rentease.exception.BookingConflictException;
import com.rentease.repository.BookingRepository;
import com.rentease.repository.ListingRepository;
import com.rentease.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BookingService bookingService;

    private User sampleCustomer;
    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User();
        sampleCustomer.setId("cust_101");
        sampleCustomer.setEmail("customer@example.com");
        sampleCustomer.setFullName("Customer User");

        sampleListing = new Listing();
        sampleListing.setId("list_101");
        sampleListing.setTitle("Studio Apartment in Indiranagar");
        sampleListing.setCategory(ListingCategory.APARTMENT);
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setPrice(new BigDecimal("25000.00"));
        sampleListing.setSecurityDeposit(new BigDecimal("50000.00"));
        sampleListing.setOwner(sampleCustomer);
    }

    @Test
    @DisplayName("Should successfully create a booking with escrow payment calculation")
    void testCreateBooking_Success() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId("list_101");
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDeliveryType("SELF_PICKUP");

        when(listingRepository.findById("list_101")).thenReturn(Optional.of(sampleListing));
        when(userRepository.findById("cust_101")).thenReturn(Optional.of(sampleCustomer));
        when(bookingRepository.findConflictingBookings(eq("list_101"), any(), any(), anyList()))
            .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId("book_999");
            return b;
        });

        BookingDto result = bookingService.createBooking("cust_101", request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("book_999");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalRentAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.getEscrowTotal()).isGreaterThan(BigDecimal.ZERO);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should prevent booking and throw BookingConflictException if listing is already reserved")
    void testCreateBooking_DatesConflict() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId("list_101");
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(5));

        when(listingRepository.findById("list_101")).thenReturn(Optional.of(sampleListing));
        when(userRepository.findById("cust_101")).thenReturn(Optional.of(sampleCustomer));

        Booking existingConflict = new Booking();
        existingConflict.setId("existing_conflict_1");
        when(bookingRepository.findConflictingBookings(eq("list_101"), any(), any(), anyList()))
            .thenReturn(List.of(existingConflict));

        assertThatThrownBy(() -> bookingService.createBooking("cust_101", request))
            .isInstanceOf(BookingConflictException.class)
            .hasMessageContaining("already booked or reserved");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if start date is after end date")
    void testCreateBooking_InvalidDates() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId("list_101");
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(2)); // End before start

        when(listingRepository.findById("list_101")).thenReturn(Optional.of(sampleListing));
        when(userRepository.findById("cust_101")).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> bookingService.createBooking("cust_101", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("End date cannot be prior to start date");

        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
