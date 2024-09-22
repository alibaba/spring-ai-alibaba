import { _getPropertyModel as _getPropertyModel_1, makeObjectEmptyValueCreator as makeObjectEmptyValueCreator_1, ObjectModel as ObjectModel_1, StringModel as StringModel_1 } from "@vaadin/hilla-lit-form";
import BookingStatusModel_1 from "../../data/BookingStatusModel.js";
import type BookingDetails_1 from "./BookingDetails.js";
class BookingDetailsModel<T extends BookingDetails_1 = BookingDetails_1> extends ObjectModel_1<T> {
    static override createEmptyValue = makeObjectEmptyValueCreator_1(BookingDetailsModel);
    get bookingNumber(): StringModel_1 {
        return this[_getPropertyModel_1]("bookingNumber", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.lang.String" } }));
    }
    get name(): StringModel_1 {
        return this[_getPropertyModel_1]("name", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.lang.String" } }));
    }
    get date(): StringModel_1 {
        return this[_getPropertyModel_1]("date", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.time.LocalDate" } }));
    }
    get bookingStatus(): BookingStatusModel_1 {
        return this[_getPropertyModel_1]("bookingStatus", (parent, key) => new BookingStatusModel_1(parent, key, false));
    }
    get from(): StringModel_1 {
        return this[_getPropertyModel_1]("from", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.lang.String" } }));
    }
    get to(): StringModel_1 {
        return this[_getPropertyModel_1]("to", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.lang.String" } }));
    }
    get bookingClass(): StringModel_1 {
        return this[_getPropertyModel_1]("bookingClass", (parent, key) => new StringModel_1(parent, key, false, { meta: { javaType: "java.lang.String" } }));
    }
}
export default BookingDetailsModel;
