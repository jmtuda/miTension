export type Reading = Readonly<{
  systolic: number;
  diastolic: number;
  pulse: number;
}>;

export type MeasurementValues = Reading;

export type FirstReadingCaptured = Readonly<{
  kind: "first-reading-captured";
  reading: Reading;
}>;

export type CalculatedMeasurement = Readonly<{
  kind: "calculated-measurement";
  first: Reading;
  second: Reading;
  result: MeasurementValues;
}>;

export type ConfirmedMeasurement = Readonly<{
  kind: "confirmed-measurement";
  values: MeasurementValues;
}>;

export function captureFirstReading(reading: Reading): FirstReadingCaptured {
  validateReading(reading);
  return { kind: "first-reading-captured", reading: { ...reading } };
}

export function addSecondReading(
  state: FirstReadingCaptured,
  second: Reading,
): CalculatedMeasurement {
  validateReading(second);

  return {
    kind: "calculated-measurement",
    first: state.reading,
    second: { ...second },
    result: {
      systolic: roundHalfUpMean(state.reading.systolic, second.systolic),
      diastolic: roundHalfUpMean(state.reading.diastolic, second.diastolic),
      pulse: roundHalfUpMean(state.reading.pulse, second.pulse),
    },
  };
}

export function confirmMeasurement(state: CalculatedMeasurement): ConfirmedMeasurement {
  return { kind: "confirmed-measurement", values: { ...state.result } };
}

export function roundHalfUpMean(first: number, second: number): number {
  assertPositiveInteger(first, "first");
  assertPositiveInteger(second, "second");

  const lower = Math.min(first, second);
  const difference = Math.max(first, second) - lower;
  return lower + Math.floor(difference / 2) + (difference % 2);
}

function validateReading(reading: Reading): void {
  assertPositiveInteger(reading.systolic, "systolic");
  assertPositiveInteger(reading.diastolic, "diastolic");
  assertPositiveInteger(reading.pulse, "pulse");

  if (reading.diastolic >= reading.systolic) {
    throw new RangeError("diastolic must be lower than systolic");
  }
}

function assertPositiveInteger(value: number, field: string): void {
  if (!Number.isSafeInteger(value) || value <= 0) {
    throw new RangeError(`${field} must be a positive integer`);
  }
}

