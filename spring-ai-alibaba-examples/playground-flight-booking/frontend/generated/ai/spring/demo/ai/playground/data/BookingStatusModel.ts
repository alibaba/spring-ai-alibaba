import { _enum as _enum_1, EnumModel as EnumModel_1, makeEnumEmptyValueCreator as makeEnumEmptyValueCreator_1 } from "@vaadin/hilla-lit-form";
import BookingStatus_1 from "./BookingStatus.js";
class BookingStatusModel extends EnumModel_1<typeof BookingStatus_1> {
    static override createEmptyValue = makeEnumEmptyValueCreator_1(BookingStatusModel);
    readonly [_enum_1] = BookingStatus_1;
}
export default BookingStatusModel;
