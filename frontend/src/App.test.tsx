import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('Commerce Console', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
  })

  it('loads paginated products and creates a product from the form', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(JSON.stringify({
        content: [{ id: 1, name: 'Shoe', sku: 'SHOE-1', description: 'A shoe', category: 'Footwear', price: 25, stock: 3, weightKg: 0.5 }],
        totalElements: 1
      }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: 2, name: 'Boot', sku: 'BOOT-1', description: 'A boot', category: 'Footwear', price: 50, stock: 2, weightKg: 0.8
      }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ content: [] }), { status: 200 }))

    render(<App />)

    expect(await screen.findByText('Shoe')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Boot' } })
    fireEvent.change(screen.getByLabelText('SKU'), { target: { value: 'BOOT-1' } })
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'Footwear' } })
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'A boot' } })
    fireEvent.change(screen.getByLabelText('Price'), { target: { value: '50' } })
    fireEvent.change(screen.getByLabelText('Stock'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Weight (kg)'), { target: { value: '0.8' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create product' }))

    await waitFor(() => expect(screen.getByText('Product created.')).toBeInTheDocument())
    expect(fetchMock).toHaveBeenCalledWith('/api/products', expect.objectContaining({ method: 'POST' }))
  })
})
