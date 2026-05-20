import {Component, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {CollectionService} from '../services/collection.service';
import {CollectionNode} from '../interfaces/collection-node';
import {CreateCollection} from '../interfaces/create-collection';
import {MatTreeModule} from '@angular/material/tree';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {
  CollectionDialogComponent,
  CollectionDialogData
} from '../dialogs/collection-dialog/collection-dialog.component';
import {UpdateCollection} from '../interfaces/update-collection';
import {filter} from 'rxjs';
import {MatMenuModule} from '@angular/material/menu';
import {RouterLink} from '@angular/router';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ConfirmationDialogComponent} from '../dialogs/confirmation-dialog/confirmation-dialog.component';
import {SelectedCollection} from '../interfaces/selected-collection';
import {
  CollectionSelectorDialogComponent,
  CollectionSelectorDialogData
} from '../dialogs/collection-selector-dialog/collection-selector-dialog.component';
import {MatSnackCommon} from '../common/mat-snack-common';
import {MatSnackBar} from '@angular/material/snack-bar';
import {CollectionBookService} from '../services/collection-book.service';
import {LibraryBookService} from '../services/library-book.service';
import {ViewBookListDialog, ViewBookListDialogData} from '../dialogs/view-book-list-dialog/view-book-list-dialog';
import {TranslocoDirective, TranslocoService} from '@jsverse/transloco';
import {MAX_COLLECTION_BOOKS} from '../common/constants';

@Component({
  selector: 'app-collections',
  standalone: true,
  imports: [
    MatTreeModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    RouterLink,
    MatTooltipModule,
    TranslocoDirective,
  ],
  templateUrl: './collections.component.html',
  styleUrl: './collections.component.scss'
})
export class CollectionsComponent implements OnInit {

  readonly SHOW_DELAY = 100;

  dataSource: CollectionNode[] = [];
  childrenAccessor = (node: CollectionNode) => node.children ?? [];
  hasChild = (_: number, node: CollectionNode) => !!node.children && node.children.length > 0;

  private snackCommon: MatSnackCommon;

  constructor(
    private collectionService: CollectionService,
    private collectionBookService: CollectionBookService,
    private libraryBookService: LibraryBookService,
    private dialog: MatDialog,
    private translocoService: TranslocoService,
    matSnackBar: MatSnackBar,
  ) {
    this.snackCommon = new MatSnackCommon(matSnackBar);
  }

  ngOnInit(): void {
    this.getTree();
  }

  private getTree(): void {
    this.collectionService.getTree().subscribe((collections: CollectionNode[]) => {
      this.dataSource = collections;
    });
  }

  openCreateDialog(parentId?: number): void {
    const dialogRef = this.dialog.open(CollectionDialogComponent, {
      data: {
        isEdit: false,
        parentId: parentId
      } as CollectionDialogData
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((collection: CreateCollection) => {
      this.collectionService.create(collection).subscribe({
        next: () => {
          this.getTree();
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.created'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  openUpdateDialog(node: CollectionNode): void {
    this.collectionService.getById(node.id).subscribe(collection => {
      const dialogRef = this.dialog.open(CollectionDialogComponent, {
        data: {
          isEdit: true,
          collection: collection
        } as CollectionDialogData
      });

      dialogRef.afterClosed().pipe(filter(Boolean)).subscribe((updatedCollection: UpdateCollection) => {
        this.collectionService.update(node.id, updatedCollection).subscribe({
          next: () => {
            this.getTree();
            this.snackCommon.showSuccess(this.translocoService.translate('collections.success.updated'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      });
    });
  }

  deleteCollection(node: CollectionNode): void {
    const message = this.translocoService.translate('collections.deleteConfirm', {name: node.name});
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        message,
        confirmLabel: 'common.delete'
      },
      width: '550px'
    });

    dialogRef.afterClosed().pipe(filter(Boolean)).subscribe(() => {
      this.collectionService.delete(node.id).subscribe({
        next: () => {
          this.getTree();
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.deleted'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  addChildCollection(targetParent: CollectionNode): void {
    const disabledIds = [
      targetParent.id,
      ...this.getAncestorIds(targetParent.id, this.dataSource),
      ...this.getSubtreeIds(targetParent)
    ];

    const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
      data: {
        initialSelectionId: null,
        disabledIds: disabledIds,
        showRoot: false
      } as CollectionSelectorDialogData
    });

    dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
      if (selection.id) {
        this.collectionService.move(selection.id, targetParent.id).subscribe({
          next: () => {
            this.getTree();
            this.snackCommon.showSuccess(this.translocoService.translate('collections.success.added'));
          },
          error: (err) => this.snackCommon.showError(err)
        });
      }
    });
  }

  moveCollection(collectionToMove: CollectionNode): void {
    const disabledIds = [collectionToMove.id, ...this.getSubtreeIds(collectionToMove)];
    if (collectionToMove.parentId) {
      disabledIds.push(collectionToMove.parentId);
    }

    const dialogRef = this.dialog.open(CollectionSelectorDialogComponent, {
      data: {
        initialSelectionId: collectionToMove.parentId || null,
        disabledIds: disabledIds,
        showRoot: true
      } as CollectionSelectorDialogData
    });

    dialogRef.afterClosed().pipe(filter(result => result !== undefined)).subscribe((selection: SelectedCollection) => {
      this.collectionService.move(collectionToMove.id, selection.id).subscribe({
        next: () => {
          this.getTree();
          this.snackCommon.showSuccess(this.translocoService.translate('collections.success.moved'));
        },
        error: (err) => this.snackCommon.showError(err)
      });
    });
  }

  openAddBookDialog(node: CollectionNode): void {
    this.collectionBookService.getCollectionBooks(node.id, {size: MAX_COLLECTION_BOOKS}).subscribe(page => {
      const data: ViewBookListDialogData = {
        libraryBooks: page.content || [],
        categoryColumn: 'categoryName',
        fetchBooksFn: (options) => this.libraryBookService.getAll(options),
      };
      const dialogRef = this.dialog.open(ViewBookListDialog, {data});
      dialogRef.afterClosed().subscribe((libraryBookId: number | undefined) => {
        if (libraryBookId) {
          this.collectionBookService.addBookToCollection(node.id, libraryBookId).subscribe({
            next: () => {
              this.snackCommon.showSuccess(this.translocoService.translate('collections.success.bookAdded'));
            },
            error: (err) => this.snackCommon.showError(err)
          });
        }
      });
    });
  }

  private getSubtreeIds(node: CollectionNode): number[] {
    let ids: number[] = [];
    if (node.children) {
      for (const child of node.children) {
        ids.push(child.id);
        ids = ids.concat(this.getSubtreeIds(child));
      }
    }
    return ids;
  }

  private getAncestorIds(targetId: number, tree: CollectionNode[]): number[] {
    const path = this.findPath(targetId, tree);
    if (path) {
      return path.map(c => c.id).filter(id => id !== targetId);
    }
    return [];
  }

  private findPath(targetId: number, currentLevel: CollectionNode[], path: CollectionNode[] = []): CollectionNode[] | null {
    for (const col of currentLevel) {
      const newPath = [...path, col];
      if (col.id === targetId) return newPath;
      if (col.children) {
        const found = this.findPath(targetId, col.children, newPath);
        if (found) return found;
      }
    }
    return null;
  }

}
