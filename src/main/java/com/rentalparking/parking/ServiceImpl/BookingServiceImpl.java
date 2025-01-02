package com.rentalparking.parking.ServiceImpl;

import com.rentalparking.parking.Entity.Booking;
import com.rentalparking.parking.Entity.Notification;
import com.rentalparking.parking.Entity.Parking;
import com.rentalparking.parking.Entity.User;
import com.rentalparking.parking.Exception.ApiException;
import com.rentalparking.parking.Repository.BookingRepository;
import com.rentalparking.parking.Repository.ParkingRepository;
import com.rentalparking.parking.Repository.UserRepository;
import com.rentalparking.parking.Service.BookingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    ParkingRepository parkingRepository;
    UserRepository userRepository;
    @Override
    public String createBooking(Booking booking) {
        String parkId= booking.getSpaceId();
        booking.setStatus("processing");
        Parking parking = parkingRepository.findById(parkId).orElseThrow(()->
                new ApiException(HttpStatus.NOT_FOUND,"Not found")
        );
        booking.setAddress(parking.getAddress());
        User usr = userRepository.findById(booking.getUserId()).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"User Not Found")
        );
        User usrOwn = userRepository.findById(parking.getUserId()).orElseThrow(
                ()-> new ApiException(HttpStatus.NOT_FOUND,"User Not found")
        );
        booking.setOwnerName(usrOwn.getName());
        booking.setUserName(usr.getName());
        Duration duration = Duration.between(booking.getStartTime(),booking.getEndTime());
        double hour=duration.toHours()*parking.getPricePerHour();
        booking.setTotalCost(String.format("%.2f", hour));
        booking.setOwner(parking.getUserId());
        return bookingRepository.save(booking).get_id() + " as booking Id placed Successfully";
    }

    @Override
    public Booking viewBooking(String id) {
        return bookingRepository.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"No Booking found"));
    }

    @Override
    public String cancelBooking(String id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"No booking found")
        );
        User user = userRepository.findById(booking.getOwner()).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"User ID Not Found")
        );
        User userBooked = userRepository.findById(booking.getUserId()).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"User ID Not Found")
        );
        List<Notification> msg =user.getInbox();
        Notification cancel = new Notification();
        cancel.setViewed(false);
        cancel.setMessage("Your booking from user "+booking.getUserName()+" cancelled the booking");
        msg.addFirst(cancel);
        user.setInbox(msg);
        userRepository.save(user);
        cancel.setMessage("Successfully cancelled booking with "+booking.getOwnerName());
        msg=userBooked.getInbox();
        msg.addFirst(cancel);
        userRepository.save(userBooked);
        bookingRepository.deleteById(id);
        return "Cancelled";
    }

    @Override
    public List<Booking> listUserBooking(String id) {
        return bookingRepository.findByUserId(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"No booking found"));
    }

    @Override
    public List<Booking> placeBooking(String id) {
        return bookingRepository.findBySpaceId(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"No booking found"));
    }

    @Override
    public String accept(String id) {
        Booking booking = viewBooking(id);
        String sts=booking.getStatus();
        booking.setStatus("accepted");
        bookingRepository.save(booking);
        return "Accepted";
    }

    @Override
    public String reject(String id) {
        Booking booking = viewBooking(id);
        String sts=booking.getStatus();
        booking.setStatus("rejected");
        bookingRepository.save(booking);
        return "Rejected";
    }

    @Override
    public List<Booking> getByOwner(String id) {
        return bookingRepository.findByOwner(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"User Not Found"));
    }

    @Override
    public String completed(String id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"Booking not found")
        );
        Parking parking = parkingRepository.findById(booking.getSpaceId()).orElseThrow(
                ()-> new ApiException(HttpStatus.NOT_FOUND,"Parking not Found")
        );
        Double cost = parking.getTotalRevenue();
        cost+= Double.parseDouble(booking.getTotalCost());
        parking.setTotalRevenue(cost);
        parkingRepository.save(parking);
        booking.setStatus("completed");
        bookingRepository.save(booking);
        return "Successfully completed.";
    }

}
