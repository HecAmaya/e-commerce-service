import type { FormEvent } from 'react'
import type { Product, ProductForm as ProductFormData } from '../services/api'

type Props = {
  editing: Product | null
  form: ProductFormData
  onChange: (key: keyof ProductFormData, value: string) => void
  onSubmit: (event: FormEvent) => void
  onCancel: () => void
}

export default function ProductForm({ editing, form, onChange, onSubmit, onCancel }: Props) {
  return <aside><div className="section-title"><h2>{editing ? 'Edit product' : 'Add product'}</h2><span>{editing ? 'Update details' : 'New inventory item'}</span></div>
    <form onSubmit={onSubmit}>
      {(['name', 'sku', 'category'] as const).map(key => <label key={key}>{key === 'sku' ? 'SKU' : key[0].toUpperCase() + key.slice(1)}<input required value={form[key]} onChange={e => onChange(key, e.target.value)} /></label>)}
      <label>Description<textarea required rows={3} value={form.description} onChange={e => onChange('description', e.target.value)} /></label>
      <div className="fields">
        <label>Price<input required min="0" step=".01" type="number" value={form.price} onChange={e => onChange('price', e.target.value)} /></label>
        <label>Stock<input required min="0" type="number" value={form.stock} onChange={e => onChange('stock', e.target.value)} /></label>
        <label>Weight (kg)<input required min="0" step=".001" type="number" value={form.weightKg} onChange={e => onChange('weightKg', e.target.value)} /></label>
      </div>
      <div className="form-actions"><button type="submit" className="primary">{editing ? 'Save changes' : 'Create product'}</button>{editing && <button type="button" onClick={onCancel}>Cancel</button>}</div>
    </form>
  </aside>
}
