import { Info, Plus, Trash2 } from 'lucide-react'
import Select from '../../../components/ui/Select.jsx'
import { Button, Empty, Input } from '../../../components/ui/Kit.jsx'
import { BLANK_BAND, SCALE } from './gradingScale.js'

/**
 * The band table, driven entirely by the scale.
 *
 * Columns, placeholders and the number `max` all come from SCALE, so a DESCRIPTOR scheme renders
 * no bound columns at all rather than empty ones the API would refuse.
 */
export default function BandEditor({ scaleType, maximumValue, rows, onChange }) {
  const scale = SCALE[scaleType]
  const measured = scale.ceiling !== null

  // Input gives onChange an EVENT; Select gives it the VALUE. Two setters rather than one that
  // guesses, because a setter reading `.target` off a string fails silently at runtime.
  const setValue = (index, field) => (value) =>
    onChange(rows.map((band, i) => (i === index ? { ...band, [field]: value } : band)))
  const setEvent = (index, field) => (event) => setValue(index, field)(event.target.value)

  const add = () => onChange([...rows, { ...BLANK_BAND }])
  const remove = (index) => onChange(rows.filter((_, i) => i !== index))

  return (
    <>
      <div className="toolbar">
        <b>Bands · {rows.length}</b>
        <span className="toolbar-spacer" />
        <Button icon={Plus} onClick={add}>Add a band</Button>
      </div>

      <p className="muted"><b>{scaleType}</b> — {scale.blurb}</p>
      <p className="muted">{scale.bandHint}</p>

      {rows.length === 0 ? (
        <Empty
          title="No bands"
          description="A scheme that grades nothing is not a scheme — an empty list is a 400, and clearing the bands is not a way to retire one."
          action={<Button look="primary" icon={Plus} onClick={add}>Add one</Button>}
        />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                {measured ? <th>From{scale.unit === '%' ? ' %' : ''}</th> : null}
                {measured ? <th>To{scale.unit === '%' ? ' %' : ''}</th> : null}
                <th>Point</th>
                <th>Description</th>
                <th>Pass</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows.map((band, index) => (
                // Keyed by position, deliberately: a band has no id and its code is the thing
                // being edited, so keying by code would remount the row on every keystroke.
                // oxlint-disable-next-line react/no-array-index-key
                <tr key={index}>
                  <td>
                    <Input value={band.gradeCode}
                      onChange={setEvent(index, 'gradeCode')} placeholder="A1" />
                  </td>
                  {measured ? (
                    <td>
                      {/* max is the scheme's own ceiling, so the browser stops a band reaching
                          past it before the API has to. */}
                      <Input type="number" min="0" max={maximumValue}
                        value={band.minimumValue}
                        onChange={setEvent(index, 'minimumValue')} placeholder={scale.from} />
                    </td>
                  ) : null}
                  {measured ? (
                    <td>
                      <Input type="number" min="0" max={maximumValue}
                        value={band.maximumValue}
                        onChange={setEvent(index, 'maximumValue')} placeholder={scale.to} />
                    </td>
                  ) : null}
                  <td>
                    <Input type="number" value={band.gradePoint}
                      onChange={setEvent(index, 'gradePoint')} placeholder="10" />
                  </td>
                  <td>
                    <Input value={band.description}
                      onChange={setEvent(index, 'description')} placeholder="Outstanding" />
                  </td>
                  <td>
                    {/* A real label, not an id: it is the accessible name of this control, and
                        "passed-3" tells a screen-reader user nothing about which row it is. */}
                    <Select value={band.passed} options={['true', 'false']}
                      label={`Pass for ${band.gradeCode || `band ${index + 1}`}`}
                      onChange={setValue(index, 'passed')} />
                  </td>
                  <td>
                    <Button icon={Trash2} onClick={() => remove(index)}>Remove</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p className="muted">
        <Info size={12} /> <b>An overlap is refused; a gap is only reported.</b> A gap means one
        mark has <i>no</i> grade — visible, and fixable. An overlap means one mark has <i>two</i>,
        and which wins depends on the order the bands happen to be stored in.
      </p>
      <p className="muted">
        <Info size={12} /> <b>A scale can never be tiled</b>, because bounds are inclusive at both
        ends: 81–90 beside 91–100 misses 90.5, and closing that is an overlap. So gaps are counted
        in <b>whole marks</b> — 90 → 91 is silent, 32 → 81 is not.
      </p>
    </>
  )
}
