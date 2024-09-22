import type BookingStatus_1 from "../../data/BookingStatus.js";
interface BookingDetails {
    bookingNumber: string;
    name: string;
    date: string;
    bookingStatus: BookingStatus_1;
    from: string;
    to: string;
    bookingClass: string;
}
export default BookingDetails;
