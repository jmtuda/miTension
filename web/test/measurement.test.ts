import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

import {
  addSecondReading,
  captureFirstReading,
  confirmMeasurement,
  roundHalfUpMean,
} from "../src/domain/measurement.ts";

test("mean follows the shared ROUND_HALF_UP contract", () => {
  for (const [first, second, expected] of contractCases()) {
    assert.equal(roundHalfUpMean(first, second), expected, `${first}, ${second}`);
  }
});

test("calculates all fields and only confirmation creates persistible type", () => {
  const first = captureFirstReading({ systolic: 121, diastolic: 79, pulse: 61 });
  const calculated = addSecondReading(first, { systolic: 122, diastolic: 82, pulse: 62 });

  assert.deepEqual(calculated.result, { systolic: 122, diastolic: 81, pulse: 62 });
  assert.deepEqual(confirmMeasurement(calculated), {
    kind: "confirmed-measurement",
    values: { systolic: 122, diastolic: 81, pulse: 62 },
  });
});

test("confirmed measurement does not retain original readings", () => {
  const confirmed = confirmMeasurement(
    addSecondReading(
      captureFirstReading({ systolic: 120, diastolic: 80, pulse: 60 }),
      { systolic: 122, diastolic: 82, pulse: 62 },
    ),
  );

  assert.deepEqual(Object.keys(confirmed).sort(), ["kind", "values"]);
});

test("validates only documented unequivocal invariants", () => {
  assert.throws(() => captureFirstReading({ systolic: 0, diastolic: 80, pulse: 60 }));
  assert.throws(() => captureFirstReading({ systolic: 120, diastolic: 0, pulse: 60 }));
  assert.throws(() => captureFirstReading({ systolic: 120, diastolic: 80, pulse: 0 }));
  assert.throws(() => captureFirstReading({ systolic: 80, diastolic: 80, pulse: 60 }));
  assert.throws(() => captureFirstReading({ systolic: 120.5, diastolic: 80, pulse: 60 }));
});

function contractCases(): Array<[number, number, number]> {
  const contractUrl = new URL("../../contracts/measurement-mean-cases.csv", import.meta.url);
  return readFileSync(fileURLToPath(contractUrl), "utf8")
    .trim()
    .split("\n")
    .slice(1)
    .map((line) => line.split(",").map(Number) as [number, number, number]);
}

