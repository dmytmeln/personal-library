import {computed, signal} from '@angular/core';

export class SelectionStore {

  private readonly _selectedIds = signal<Set<number>>(new Set());

  readonly selectedIds = computed(() => Array.from(this._selectedIds()));
  readonly count = computed(() => this._selectedIds().size);
  readonly isSelectionMode = computed(() => this.count() > 0);

  isSelected(id: number): boolean {
    return this._selectedIds().has(id);
  }

  toggle(id: number): void {
    this._selectedIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  clear(): void {
    this._selectedIds.set(new Set());
  }

}
