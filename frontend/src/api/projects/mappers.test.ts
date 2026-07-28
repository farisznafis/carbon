import { mapProjectListItem } from './mappers';

describe('mapProjectListItem', () => {
  it('preserves the backend Project public ID', () => {
    const project = mapProjectListItem({
      id: 'PROJ0123',
      projectName: '太陽光発電プロジェクト',
      projectStatus: 'ACTIVE',
      methodologyCategory: 'EN-R',
      methodologyNumber: '002',
      methodologyName: '太陽光発電設備の導入',
      methodologyMainEmission: '排出削減活動',
      methodologyVersion: '1.0',
      methodCombiFeasibility: false,
    });

    expect(project.id).toBe('PROJ0123');
  });
});
